## RISC-V-Pipeline-Simulator

This is a RISC-V pipelined processor simulator implemented in Java. It can load a RISC-V assembly program and simulate the execution cycle-by-cycle, showing the contents of all pipeline stages and components.

### Features

* Supports full RISC-V integer instruction set
* Models 5-stage pipeline with fetch, decode, execute, memory access, and writeback stages
* Implements hazard detection and handling
* Prints simulation trace showing status of all pipeline components per clock cycle
* Handles branches and jumps properly
