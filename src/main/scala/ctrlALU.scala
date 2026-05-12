package riscv

import chisel3._
import chisel3.util._

class ALU_CTRL_RV() extends Module {
    val io = IO(new Bundle {
        val funct_3 = Input(UInt(3.W))
        val funct_7 = Input(UInt(7.W))
        val ctrl = Input(UInt(2.W))
        val ula_opcode = Output(UInt(4.W))
    })

    val opcode = Wire(UInt(4.W))

    switch(io.ctrl) {
        is (0.U(2.W)) {
            opcode := 0.U(4.W)
        }
        is (1.U(2.W)) {
            opcode := 1.U(4.W)
        }
        is (2.U(2.W)) {
            when(io.funct_3 === "b000".U & io.funct_7 === "b0000000".U) {
                opcode := 0.U(4.W) // ADD
            }
            .elsewhen(io.funct_3 === "b000".U & io.funct_7 === "b0100000".U) {
                opcode := 1.U(4.W) // SUB
            }
            .elsewhen(io.funct_3 === "b111".U & io.funct_7 === "b0000000".U) {
                opcode := 2.U(4.W) // AND
            }
            .elsewhen(io.funct_3 === "b110".U & io.funct_7 === "b0000000".U) {
                opcode := 3.U(4.W) // OR
            }
            .elsewhen(io.funct_3 === "b100".U & io.funct_7 === "b0000000".U) {
                opcode := 4.U(4.W) // XOR
            }
            .elsewhen(io.funct_3 === "b001".U & io.funct_7 === "b0000000".U) {
                opcode := 5.U(4.W) // SLL
            }
            .elsewhen(io.funct_3 === "b101".U & io.funct_7 === "b0000000".U) {
                opcode := 6.U(4.W) // SRL
            }
            .elsewhen(io.funct_3 === "b101".U & io.funct_7 === "b0100000".U) {
                opcode := 7.U(4.W) // SRA
            }
            .elsewhen(io.funct_3 === "b010".U & io.funct_7 === "b0000000".U) {
                opcode := 8.U(4.W) // SLT
            }
            .elsewhen(io.funct_3 === "b011".U & io.funct_7 === "b0000000".U) {
                opcode := 9.U(4.W) // SLTU
            }
            .elsewhen(io.funct_3 === "b101".U & io.funct_7 === "b0000000".U) {
                opcode := 10.U(4.W) // SGE
            }
            .elsewhen(io.funct_3 === "b111".U & io.funct_7 === "b0000000".U) {
                opcode := 11.U(4.W) // SGEU
            }
            .elsewhen(io.funct_3 === "b000".U & io.funct_7 === "b0000000".U) {
                opcode := 12.U(4.W) // SWQ
            }
            .elsewhen(io.funct_3 === "b001".U & io.funct_7 === "b0000000".U) {
                opcode := 13.U(4.W) // SNE
            }
            .otherwise {
                opcode := 0.U(4.W)
            }
        }
        is (3.U(2.W)) {
            when(io.funct_3 === "b000".U) {
                opcode := 0.U(4.W) // ADDI
            }
            .elsewhen(io.funct_3 === "b100".U) {
                opcode := 4.U(4.W) // XORI
            }
            .elsewhen(io.funct_3 === "b110".U) {
                opcode := 3.U(4.W) // ORI
            }
            .elsewhen(io.funct_3 === "b111".U) {
                opcode := 2.U(4.W) // ANDI
            }
        }
    }

    io.ula_opcode := opcode
}