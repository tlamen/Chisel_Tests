// src/main/scala/riscv/IMEM.scala
package riscv

import chisel3._
import chisel3.util._

class IMEM(val mem_size: Int = 1024) extends Module {
  // Calculate address width needed to address mem_size words (32-bit each)
  val addr_width = log2Ceil(mem_size)
  
  val io = IO(new Bundle {
    // Control signals
    val write_enable = Input(Bool())
    val read_enable  = Input(Bool())
    
    // Address (enough bits to reach mem_size)
    val address = Input(UInt(addr_width.W))
    
    // Data
    val data_in  = Input(UInt(32.W))
    val data_out = Output(UInt(32.W))
  })

  // Memory array: mem_size words of 32 bits each
  val mem = Mem(mem_size, UInt(32.W))
  
  // Write operation (synchronous)
  when(io.write_enable) {
    mem.write(io.address, io.data_in)
  }
  
  // Read operation (synchronous)
  val readData = RegNext(mem.read(io.address), 0.U(32.W))
  io.data_out := Mux(io.read_enable, readData, 0.U(32.W))
}