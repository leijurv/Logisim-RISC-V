# Logisim-RISC-V

This repo implements a RISC-V processor in the circuit simulation software Logisim. 

`RV32I-vanilla-logisim.circ` implements almost all of RV32I, but does not support misaligned reads and writes of multi byte values in RAM. This is due to a limitation in the RAM module in Logisim itself. In practice, this doesn't affect normal programs, but I've created a fork of Logisim that allows for this. The only modification is in `Logisim/src/com/cburch/logisim/std/memory/Ram.java`, starting around line 201.

`RV32I-modded-logisim.circ` is the version that's actively developed

`vanilla-logisim-2.7.1.jar` is provided for convenience, and my modification to Logisim can be found at `Logisim/dist/Logisim.jar`


# Running a program
This repo contains an example program (`sample.c`) and its compiled version (`samplerom.image`) as a sample (without needing to install the RISC-V GCC). The program demonstrates loops, function calls, floating point emulation, recursion, and memory allocation.

First, start Logisim. This example will work with either the modified Logisim `java -jar Logisim/dist/Logisim.jar` or vanilla Logisim `java -jar vanilla-logisim-2.7.1.jar`.

Open `RV32I-modded-logisim.circ` (or `RV32I-vanilla-logisim.circ`). 

Right click the ROM module on the far left, select Clear contents. Right click it again and select Load image and select `samplerom.image` from this directory (or another `rom.image`, for a custom program).

Do the same for the RAM module on the top right.

If you're running a custom program, you'll need to find the correct `_start` address and put it in the constant on the left. If you're just running the sample program, leave it at `00010260` as that's correct.

Using the hand tool, turn on the toggle pin on the far far left, then turn on and off the clock on the far top left, then turn off the toggle pin. This overrides the jump evaluator temporarily and sets the program counter to the correct address for `_start`. 

Under Simulate -> Tick Frequency, you may want to select the highest tick speed. 

Hit Command+K to start the CPU.

To view register values in real time, right click either of the two central RAM modules and select edit contents.

Once the program finishes, (depending on the program) the result can be seen in the contents of the registers, or program RAM. 

On my laptop, which runs this CPU at around 400Hz, `samplerom.image` only takes a minute or two.

For `samplerom.image`, the final output can be seen by editing contents of program RAM (top right) and reading the first row, starting at the third column. It should be `46c23 46c28 46c2e 46c34 46c3a 46c3f 46c45 46c4b 46c51 46c56`, which is the correct output for the C program, as can be verified by running `sample.c` normally with the printf uncommented.



# Setup and installation

The RISC-V GNU toolchain needs to be installed, in order to build new C programs. Instructions can be found here: https://riscv.org/software-tools/risc-v-gnu-compiler-toolchain/

First install the prerequisites (from that link). On Mac OSX, there's one extra one that wasn't mentioned, `brew install libtool`.

Then, configuration / build / install steps that worked for me were:

```
cd Downloads
git clone --recursive https://github.com/riscv/riscv-gnu-toolchain
cd riscv-gnu-toolchain
./configure --prefix=/opt/riscv --with-arch=rv32i
sudo make
```

This will place the required binaries in `/opt/riscv`. Make sure to include `--with-arch=rv32i`. 

# Compiling a C program

There are several steps in going from a C program to a `rom.image` that can be loaded into Logisim.

First, compile the program to a RISC-V ELF binary: `/opt/riscv/bin/riscv32-unknown-elf-gcc -march=rv32i -mabi=ilp32 -O3 -o test test.c`

Then, convert that binary to a `rom.image` that Logisim can read: `/opt/riscv/bin/riscv32-unknown-elf-objdump -s test | java -jar BinaryConverter/dist/BinaryConverter.jar > rom.image`

Then, locate the address of `_start`, which will be needed to run it (for `sample.c` this is `00010260`): `/opt/riscv/bin/riscv32-unknown-elf-readelf -a test | grep " _start"`

Finally, follow the steps in `Running a program` with this `rom.image` and `_start` address.


# Logisim and BinaryConverter

These are NetBeans projects, so they can be built in the IDE, or from the command line using `ant jar`. Both are pre-built in this repo.

# License

Logisim is GPL licenced, therefore so is this project.