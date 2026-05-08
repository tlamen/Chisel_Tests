// TopModule.scala
package riscv

import chisel3._
import chisel3.util._

// Control signals for the simple processor
class ControlUnit extends Bundle {
  val regWrite = Bool()
  val aluOpcode = UInt(4.W)
  val aluSrc = Bool()  // 0 = use rs2, 1 = use immediate
  val memWrite = Bool()
  val memRead = Bool()
}

// Simple Top Module integrating Register Bank and ALU
class TopModule(val WSIZE: Int = 32) extends Module {
  val io = IO(new Bundle {
    // External control
    val enable = Input(Bool())
    
    // Instruction execution control
    val instruction = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
    
    // Immediate value for operations
    val immediate = Input(UInt(WSIZE.W))
    
    // Outputs for debugging/verification
    val aluResult = Output(UInt(WSIZE.W))
    val regRead1 = Output(UInt(WSIZE.W))
    val regRead2 = Output(UInt(WSIZE.W))
    val condFlag = Output(Bool())
    
    // Memory interface (simplified)
    val memReadData = Input(UInt(WSIZE.W))
    val memWriteData = Output(UInt(WSIZE.W))
    val memAddress = Output(UInt(WSIZE.W))
  })
  
  // Instantiate Register Bank (no explicit clk needed)
  val regfile = Module(new XREGS(WSIZE))
  
  // Instantiate ALU
  val alu = Module(new ALU_RV(WSIZE))
  
  // Decode instruction fields (RISC-V RV32I format)
  val opcode = io.instruction(6,0)
  val rs1 = io.instruction(19,15)
  val rs2 = io.instruction(24,20)
  val rd = io.instruction(11,7)
  val funct3 = io.instruction(14,12)
  val funct7 = io.instruction(31,25)
  
  // Control signal generation (simplified)
  val control = Wire(new ControlUnit)
  
  // Default control values
  control.regWrite := false.B
  control.aluOpcode := 0.U
  control.aluSrc := false.B
  control.memWrite := false.B
  control.memRead := false.B
  
  // Pipeline registers for sequential operation
  val stage1_regWrite = RegNext(control.regWrite)
  val stage1_rd = RegNext(rd)
  val stage1_aluResult = RegNext(alu.io.Z)
  
  // Decode based on opcode (combinational)
  when(io.enable) {
    switch(opcode) {
      // R-type instructions (ADD, SUB, AND, OR, etc.)
      is("b0110011".U) {
        control.regWrite := true.B
        control.aluSrc := false.B
        // Map funct3 and funct7 to ALU opcode
        when(funct3 === 0.U) {
          when(funct7 === 0.U) { control.aluOpcode := alu.ADD_OP }
          when(funct7 === "b0100000".U) { control.aluOpcode := alu.SUB_OP }
        }.elsewhen(funct3 === 1.U) { control.aluOpcode := alu.SLL_OP }
        .elsewhen(funct3 === 2.U) { control.aluOpcode := alu.SLT_OP }
        .elsewhen(funct3 === 3.U) { control.aluOpcode := alu.SLTU_OP }
        .elsewhen(funct3 === 4.U) { control.aluOpcode := alu.XOR_OP }
        .elsewhen(funct3 === 5.U) {
          when(funct7 === 0.U) { control.aluOpcode := alu.SRL_OP }
          when(funct7 === "b0100000".U) { control.aluOpcode := alu.SRA_OP }
        }
        .elsewhen(funct3 === 6.U) { control.aluOpcode := alu.OR_OP }
        .elsewhen(funct3 === 7.U) { control.aluOpcode := alu.AND_OP }
      }
      
      // I-type instructions (ADDI, etc.)
      is("b0010011".U) {
        control.regWrite := true.B
        control.aluSrc := true.B
        when(funct3 === 0.U) { control.aluOpcode := alu.ADD_OP }
        .elsewhen(funct3 === 4.U) { control.aluOpcode := alu.XOR_OP }
        .elsewhen(funct3 === 6.U) { control.aluOpcode := alu.OR_OP }
        .elsewhen(funct3 === 7.U) { control.aluOpcode := alu.AND_OP }
        .elsewhen(funct3 === 1.U) { control.aluOpcode := alu.SLL_OP }
        .elsewhen(funct3 === 5.U) { control.aluOpcode := alu.SRL_OP }
      }
      
      // Load instructions
      is("b0000011".U) {
        control.regWrite := true.B
        control.memRead := true.B
        control.aluOpcode := alu.ADD_OP
        control.aluSrc := true.B
      }
      
      // Store instructions
      is("b0100011".U) {
        control.memWrite := true.B
        control.aluOpcode := alu.ADD_OP
        control.aluSrc := true.B
      }
      
      // Branch instructions (simplified)
      is("b1100011".U) {
        control.aluOpcode := alu.SEQ_OP
        when(funct3 === 0.U) { control.aluOpcode := alu.SEQ_OP }  // BEQ
        .elsewhen(funct3 === 1.U) { control.aluOpcode := alu.SNE_OP }  // BNE
        .elsewhen(funct3 === 4.U) { control.aluOpcode := alu.SLT_OP }  // BLT
        .elsewhen(funct3 === 5.U) { control.aluOpcode := alu.SGE_OP }  // BGE
        .elsewhen(funct3 === 6.U) { control.aluOpcode := alu.SLTU_OP } // BLTU
        .elsewhen(funct3 === 7.U) { control.aluOpcode := alu.SGEU_OP } // BGEU
      }
    }
  }
  
  // Connect Register Bank outputs
  regfile.io.rs1 := rs1
  regfile.io.rs2 := rs2
  
  // ALU input selection
  alu.io.A := regfile.io.ro1
  alu.io.B := Mux(control.aluSrc, io.immediate, regfile.io.ro2)
  alu.io.opcode := control.aluOpcode
  
  // ALU result output
  io.aluResult := alu.io.Z
  io.condFlag := alu.io.cond
  
  // Register write back (using pipelined values)
  regfile.io.rd := stage1_rd
  regfile.io.data := Mux(control.memRead, io.memReadData, stage1_aluResult)
  regfile.io.wren := stage1_regWrite && io.enable
  
  // Memory interface
  io.memAddress := alu.io.Z
  io.memWriteData := regfile.io.ro2
  
  // Debug outputs
  io.regRead1 := regfile.io.ro1
  io.regRead2 := regfile.io.ro2
}