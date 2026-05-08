// src/main/scala/riscv/XREGS.scala
package riscv

import chisel3._
import chisel3.util._

class XREGS(val WSIZE: Int = 32) extends Module {
  val io = IO(new Bundle {
    val wren  = Input(Bool())
    val rs1   = Input(UInt(5.W))
    val rs2   = Input(UInt(5.W))
    val rd    = Input(UInt(5.W))
    val data  = Input(UInt(WSIZE.W))
    val ro1   = Output(UInt(WSIZE.W))
    val ro2   = Output(UInt(WSIZE.W))
  })

  // Register file: 32 registers of WSIZE bits each
  val regfile = RegInit(VecInit(Seq.fill(32)(0.U(WSIZE.W))))
  
  // Register 0 is hardwired to 0
  regfile(0) := 0.U
  
  // Write operation (on rising clock edge)
  when(io.wren && io.rd =/= 0.U) {
    regfile(io.rd) := io.data
  }
  
  // Read operations with forced zero for register 0
  io.ro1 := Mux(io.rs1 === 0.U, 0.U, regfile(io.rs1))
  io.ro2 := Mux(io.rs2 === 0.U, 0.U, regfile(io.rs2))
}