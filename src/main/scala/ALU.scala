// ALU_RV.scala
package riscv

import chisel3._
import chisel3.util._

class ALU_RV(val WSIZE: Int = 32) extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(4.W))
    val A      = Input(UInt(WSIZE.W))
    val B      = Input(UInt(WSIZE.W))
    val Z      = Output(UInt(WSIZE.W))
    val cond   = Output(Bool())
  })

  // Operation codes (from specification)
  val ADD_OP  = 0.U(4.W)
  val SUB_OP  = 1.U(4.W)
  val AND_OP  = 2.U(4.W)
  val OR_OP   = 3.U(4.W)
  val XOR_OP  = 4.U(4.W)
  val SLL_OP  = 5.U(4.W)
  val SRL_OP  = 6.U(4.W)
  val SRA_OP  = 7.U(4.W)
  val SLT_OP  = 8.U(4.W)
  val SLTU_OP = 9.U(4.W)
  val SGE_OP  = 10.U(4.W)
  val SGEU_OP = 11.U(4.W)
  val SEQ_OP  = 12.U(4.W)
  val SNE_OP  = 13.U(4.W)

  // Convert to signed for arithmetic operations
  val signedA = io.A.asSInt
  val signedB = io.B.asSInt
  val unsignedA = io.A
  val unsignedB = io.B

  // Temporary result and condition
  val result = Wire(UInt(WSIZE.W))
  val condition = Wire(Bool())
  
  // Main ALU logic
  result := 0.U
  condition := false.B
  
  // Combinational logic for ALU operations
  switch(io.opcode) {
    is(ADD_OP) {
      result := (signedA + signedB).asUInt
    }
    is(SUB_OP) {
      result := (signedA - signedB).asUInt
    }
    is(AND_OP) {
      result := io.A & io.B
    }
    is(OR_OP) {
      result := io.A | io.B
    }
    is(XOR_OP) {
      result := io.A ^ io.B
    }
    is(SLL_OP) {
      // Shift left logical
      val shiftAmount = io.B(4,0).asUInt
      result := (io.A << shiftAmount)(WSIZE-1,0)
    }
    is(SRL_OP) {
      // Shift right logical
      val shiftAmount = io.B(4,0).asUInt
      result := (io.A >> shiftAmount)
    }
    is(SRA_OP) {
      // Shift right arithmetic
      val shiftAmount = io.B(4,0).asUInt
      result := (signedA >> shiftAmount).asUInt
    }
    is(SLT_OP) {
      // Set less than (signed)
      result := Mux(signedA < signedB, 1.U, 0.U)
      condition := (signedA < signedB)
    }
    is(SLTU_OP) {
      // Set less than (unsigned)
      result := Mux(unsignedA < unsignedB, 1.U, 0.U)
      condition := (unsignedA < unsignedB)
    }
    is(SGE_OP) {
      // Set greater or equal (signed)
      result := Mux(signedA >= signedB, 1.U, 0.U)
      condition := (signedA >= signedB)
    }
    is(SGEU_OP) {
      // Set greater or equal (unsigned)
      result := Mux(unsignedA >= unsignedB, 1.U, 0.U)
      condition := (unsignedA >= unsignedB)
    }
    is(SEQ_OP) {
      // Set equal
      result := Mux(io.A === io.B, 1.U, 0.U)
      condition := (io.A === io.B)
    }
    is(SNE_OP) {
      // Set not equal
      result := Mux(io.A =/= io.B, 1.U, 0.U)
      condition := (io.A =/= io.B)
    }
  }
  
  io.Z := result
  io.cond := condition
}