/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */
package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class ECall extends InstanceFactory {

    public static MemContents mainRam;
    public static MemContents registers;
    public static int brk = 5;

    private static final int CLR = 0;
    private static final int CK = 1;
    private static final int WE = 2;
    private static final int IN = 3;

    private static final int BORDER = 5;
    private static final int ROW_HEIGHT = 15;
    private static final int COL_WIDTH = 7;
    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 64);

    private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

    private static final Attribute<Integer> ATTR_COLUMNS
            = Attributes.forIntegerRange("cols",
                    Strings.getter("ttyColsAttr"), 1, 120);
    private static final Attribute<Integer> ATTR_ROWS
            = Attributes.forIntegerRange("rows",
                    Strings.getter("ttyRowsAttr"), 1, 48);

    public ECall() {
        super("ECall", Strings.getter("ecallComponent"));
        setAttributes(new Attribute[]{
            ATTR_ROWS, ATTR_COLUMNS, StdAttr.EDGE_TRIGGER,
            Io.ATTR_COLOR, Io.ATTR_BACKGROUND
        }, new Object[]{
            Integer.valueOf(8), Integer.valueOf(32), StdAttr.TRIG_RISING,
            Color.BLACK, DEFAULT_BACKGROUND
        });
        setIconName("tty.gif");

        Port[] ps = new Port[4];
        ps[CLR] = new Port(20, 10, Port.INPUT, 1);
        ps[CK] = new Port(0, 0, Port.INPUT, 1);
        ps[WE] = new Port(10, 10, Port.INPUT, 1);
        ps[IN] = new Port(0, -10, Port.INPUT, 7);
        ps[CLR].setToolTip(Strings.getter("ttyClearTip"));
        ps[CK].setToolTip(Strings.getter("ttyClockTip"));
        ps[WE].setToolTip(Strings.getter("ttyEnableTip"));
        ps[IN].setToolTip(Strings.getter("ttyInputTip"));
        setPorts(ps);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        int rows = getRowCount(attrs.getValue(ATTR_ROWS));
        int cols = getColumnCount(attrs.getValue(ATTR_COLUMNS));
        int width = 2 * BORDER + cols * COL_WIDTH;
        int height = 2 * BORDER + rows * ROW_HEIGHT;
        if (width < 30) {
            width = 30;
        }
        if (height < 30) {
            height = 30;
        }
        return Bounds.create(0, 10 - height, width, height);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == ATTR_ROWS || attr == ATTR_COLUMNS) {
            instance.recomputeBounds();
        }
    }

    private static int decode(byte[] bi) { // https://stackoverflow.com/questions/11421905/java-integer-to-byte-and-byte-to-integer
        return bi[3] & 0xFF | (bi[2] & 0xFF) << 8
                | (bi[1] & 0xFF) << 16 | (bi[0] & 0xFF) << 24;
    }

    private static byte[] encode(int i) {
        return new byte[]{(byte) (i >>> 24), (byte) ((i << 8) >>> 24),
            (byte) ((i << 16) >>> 24), (byte) ((i << 24) >>> 24)
        };
    }

    @Override
    public void propagate(InstanceState circState) {

        Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
        TtyState state = getTtyState(circState);
        Value clear = circState.getPort(CLR);
        Value clock = circState.getPort(CK);
        Value enable = circState.getPort(WE);
        Value in = circState.getPort(IN);

        synchronized (state) {
            Value lastClock = state.setLastClock(clock);
            if (clear == Value.TRUE) {
                state.clear();
            } else if (enable != Value.FALSE) {
                boolean go;
                if (trigger == StdAttr.TRIG_FALLING) {
                    go = lastClock == Value.TRUE && clock == Value.FALSE;
                } else {
                    go = lastClock == Value.FALSE && clock == Value.TRUE;
                }
                if (go) {
                    System.out.println("Propagating to an ecall");
                    MemContents reg = registers;
                    MemContents ram = mainRam;
                    if (reg != null && ram != null) {
                        int syscallType = reg.get(17);
                        System.out.println("Syscall " + syscallType);
                        if (syscallType == 93) {
                            System.out.println("Program is exiting");
                        }
                        if (syscallType == 80) {
                            System.out.println("Fstat " + reg.get(10) + " " + reg.get(11) + " " + reg.get(12));
                            byte[] data = {-96, 46, 117, 67, 41, 7, 0, 0, -112, 33, 1, 0, -11, 1, 0, 0, 4, 0, 0, 0, 5, 0, 0, 16, 119, -123, -62, 90, 0, 0, 0, 0, 104, -91, 81, 15, 0, 0, 0, 0, 119, -123, -62, 90, 0, 0, 0, 0, -56, 23, 102, 15, 0, 0, 0, 0, 119, -123, -62, 90, 0, 0, 0, 0, -56, 23, 102, 15};

                            for (int i = 0; i < data.length; i++) {
                                //write toStore[i] to addr+i
                                int loc = reg.get(11) + i;
                                byte[] tmp2 = encode(loc);
                                tmp2[0] = 0;
                                loc = decode(tmp2);

                                int val = mainRam.get(loc / 4);
                                byte[] v = encode(val);
                                v[3 - loc % 4] = data[i];
                                //v[loc % 4] = toStore[i];
                                //System.out.println("Writing " + decode(v) + " to " + loc / 4);
                                mainRam.set(loc / 4, decode(v));
                            }
                            reg.set(10, 0);
                        }
                        if (syscallType == 214) {

                            int res = brk;
                            brk = res + reg.get(10);
                            System.out.println("SBRK " + reg.get(10) + " " + res + " " + brk);
                            reg.set(10, res);
                        }

                        if (syscallType == 64) {

                            int fd = reg.get(10);
                            int pointer = reg.get(11);
                            byte[] tmp = encode(pointer);
                            tmp[0] = 0;
                            pointer = decode(tmp);
                            int len = reg.get(12);
                            System.out.println("Writing " + fd + " " + pointer + " " + len);
                            for (int i = 0; i < len; i++) {
                                char c = (char) encode(ram.get((pointer + i) / 4))[3 - (pointer + i) % 4];
                                System.out.print(c);
                                state.add(c);
                            }
                        }
                        /*for (int i = 0; i < 32; i++) {
                            System.out.println("Register " + i + ": " + reg.get(i));
                        }*/
                    }
                    //state.add(in.isFullyDefined() ? (char) in.toIntValue() : '?');
                }
            }
        }
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        GraphicsUtil.switchToWidth(g, 2);
        Bounds bds = painter.getBounds();
        g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(),
                10, 10);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        boolean showState = painter.getShowState();
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();
        painter.drawClock(CK, Direction.EAST);
        if (painter.shouldDrawColor()) {
            g.setColor(painter.getAttributeValue(Io.ATTR_BACKGROUND));
            g.fillRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(),
                    10, 10);
        }
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(),
                2 * BORDER, 2 * BORDER);
        GraphicsUtil.switchToWidth(g, 1);
        painter.drawPort(CLR);
        painter.drawPort(WE);
        painter.drawPort(IN);

        int rows = getRowCount(painter.getAttributeValue(ATTR_ROWS));
        int cols = getColumnCount(painter.getAttributeValue(ATTR_COLUMNS));

        if (showState) {
            String[] rowData = new String[rows];
            int curRow;
            int curCol;
            TtyState state = getTtyState(painter);
            synchronized (state) {
                for (int i = 0; i < rows; i++) {
                    rowData[i] = state.getRowString(i);
                }
                curRow = state.getCursorRow();
                curCol = state.getCursorColumn();
            }

            g.setFont(DEFAULT_FONT);
            g.setColor(painter.getAttributeValue(Io.ATTR_COLOR));
            FontMetrics fm = g.getFontMetrics();
            int x = bds.getX() + BORDER;
            int y = bds.getY() + BORDER + (ROW_HEIGHT + fm.getAscent()) / 2;
            for (int i = 0; i < rows; i++) {
                g.drawString(rowData[i], x, y);
                if (i == curRow) {
                    int x0 = x + fm.stringWidth(rowData[i].substring(0, curCol));
                    g.drawLine(x0, y - fm.getAscent(), x0, y);
                }
                y += ROW_HEIGHT;
            }
        } else {
            String str = Strings.get("ttyDesc", "" + rows, "" + cols);
            FontMetrics fm = g.getFontMetrics();
            int strWidth = fm.stringWidth(str);
            if (strWidth + BORDER > bds.getWidth()) {
                str = Strings.get("ttyDescShort");
                strWidth = fm.stringWidth(str);
            }
            int x = bds.getX() + (bds.getWidth() - strWidth) / 2;
            int y = bds.getY() + (bds.getHeight() + fm.getAscent()) / 2;
            g.drawString(str, x, y);
        }
    }

    private TtyState getTtyState(InstanceState state) {
        int rows = getRowCount(state.getAttributeValue(ATTR_ROWS));
        int cols = getColumnCount(state.getAttributeValue(ATTR_COLUMNS));
        TtyState ret = (TtyState) state.getData();
        if (ret == null) {
            ret = new TtyState(rows, cols);
            state.setData(ret);
        } else {
            ret.updateSize(rows, cols);
        }
        return ret;
    }

    public void sendToStdout(InstanceState state) {
        TtyState tty = getTtyState(state);
        tty.setSendStdout(true);
    }

    private static int getRowCount(Object val) {
        if (val instanceof Integer) {
            return ((Integer) val).intValue();
        } else {
            return 4;
        }
    }

    private static int getColumnCount(Object val) {
        if (val instanceof Integer) {
            return ((Integer) val).intValue();
        } else {
            return 16;
        }
    }
}
