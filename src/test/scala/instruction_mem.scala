// src/test/scala/riscv/IMEMTest.scala
package riscv

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class IMEMTest extends AnyFunSpec with ChiselSim with Matchers {
  describe("Instruction Memory") {
    
    it("should write and read a single instruction") {
      simulate(new IMEM(mem_size = 256)) { dut =>
        // Write instruction at address 0
        dut.io.write_enable.poke(true.B)
        dut.io.read_enable.poke(false.B)
        dut.io.address.poke(0.U)
        dut.io.data_in.poke(0x00500093.U)  // addi x1, x0, 5
        dut.clock.step()
        
        // Disable write, enable read
        dut.io.write_enable.poke(false.B)
        dut.io.read_enable.poke(true.B)
        dut.io.address.poke(0.U)
        dut.clock.step()
        
        // Verify read data
        dut.io.data_out.expect(0x00500093.U)
        
        info("✓ Write and read successful")
      }
    }
    
    it("should write and read multiple instructions at different addresses") {
      simulate(new IMEM(mem_size = 256)) { dut =>
        val testInstructions = Seq(
          (0, 0x00500093),   // addi x1, x0, 5
          (4, 0x00A00113),   // addi x2, x0, 10
          (8, 0x002081B3),   // add x3, x1, x2
          (12, 0x00000063)   // beq x0, x0, 0
        )
        
        // Write all instructions
        for ((addr, inst) <- testInstructions) {
          dut.io.write_enable.poke(true.B)
          dut.io.read_enable.poke(false.B)
          dut.io.address.poke(addr.U)
          dut.io.data_in.poke(inst.U)
          dut.clock.step()
        }
        
        // Read and verify all instructions
        for ((addr, inst) <- testInstructions) {
          dut.io.write_enable.poke(false.B)
          dut.io.read_enable.poke(true.B)
          dut.io.address.poke(addr.U)
          dut.clock.step()
          dut.io.data_out.expect(inst.U)
        }
        
        info("✓ Multiple instructions written and read successfully")
      }
    }
    
    it("should output zero when read_enable is false") {
      simulate(new IMEM(mem_size = 256)) { dut =>
        // Write a value
        dut.io.write_enable.poke(true.B)
        dut.io.read_enable.poke(false.B)
        dut.io.address.poke(0.U)
        dut.io.data_in.poke(0xDEADBEEFL.U)
        dut.clock.step()
        
        // Read with read_enable = false
        dut.io.write_enable.poke(false.B)
        dut.io.read_enable.poke(false.B)
        dut.io.address.poke(0.U)
        dut.clock.step()
        
        // Should output 0
        dut.io.data_out.expect(0.U)
        
        info("✓ Output zero when read disabled")
      }
    }
    
    it("should preserve data when not writing") {
      simulate(new IMEM(mem_size = 256)) { dut =>
        // Write initial value
        dut.io.write_enable.poke(true.B)
        dut.io.address.poke(0.U)
        dut.io.data_in.poke(0x12345678.U)
        dut.clock.step()
        
        // Try to write with write_enable = false
        dut.io.write_enable.poke(false.B)
        dut.io.data_in.poke("h87654321".U)  // Different value
        dut.clock.step()
        
        // Read back - should still be original value
        dut.io.read_enable.poke(true.B)
        dut.io.address.poke(0.U)
        dut.clock.step()
        dut.io.data_out.expect(0x12345678.U)
        
        info("✓ Data preserved when write disabled")
      }
    }
    
    it("should handle address boundary correctly") {
      simulate(new IMEM(mem_size = 64)) { dut =>
        val maxAddr = 63  // mem_size - 1
        
        // Write at maximum address
        dut.io.write_enable.poke(true.B)
        dut.io.address.poke(maxAddr.U)
        dut.io.data_in.poke("hCAFEBABE".U)
        dut.clock.step()
        
        // Read back from maximum address
        dut.io.write_enable.poke(false.B)
        dut.io.read_enable.poke(true.B)
        dut.io.address.poke(maxAddr.U)
        dut.clock.step()
        dut.io.data_out.expect("hCAFEBABE".U)
        
        info("✓ Address boundary handling correct")
      }
    }
    
    it("should overwrite existing data when writing to same address") {
      simulate(new IMEM(mem_size = 256)) { dut =>
        // Write first value
        dut.io.write_enable.poke(true.B)
        dut.io.address.poke(10.U)
        dut.io.data_in.poke(0x11111111.U)
        dut.clock.step()
        
        // Write second value (overwrite)
        dut.io.data_in.poke(0x22222222.U)
        dut.clock.step()
        
        // Read back - should be second value
        dut.io.write_enable.poke(false.B)
        dut.io.read_enable.poke(true.B)
        dut.io.address.poke(10.U)
        dut.clock.step()
        dut.io.data_out.expect(0x22222222.U)
        
        info("✓ Overwrite works correctly")
      }
    }
  }
}