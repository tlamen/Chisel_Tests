package riscv

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

class DMEMTest extends AnyFunSpec with ChiselSim with Matchers {
  describe("DMEM Word-Addressable Data Memory") {
    
    // Helper function to convert hex string to Int (for arithmetic)
    def h2i(hex: String): Int = Integer.parseInt(hex.replaceAll("h", ""), 16)
    
    // Alternative: Define constants for repeated values
    val PATTERN_01010101 = 0x01010101
    val PATTERN_10000 = 0x10000

    it("should handle address 0 correctly after multiple operations") {
      simulate(new DMEM(mem_size = 256)) { dut =>
        // Write to address 0
        dut.io.write_enable.poke(true.B)
        dut.io.address.poke(0.U)
        dut.io.data_in.poke("h11111111".U)  // String to UInt is fine here
        dut.clock.step()
        
        // Write to other addresses - use numeric literal for arithmetic
        for (addr <- 1 to 10) {
          dut.io.address.poke(addr.U)
          // CORRECTED: Use numeric literal, not String
          dut.io.data_in.poke((addr * PATTERN_01010101).U)
          dut.clock.step()
        }
        
        // Read back address 0
        dut.io.write_enable.poke(false.B)
        dut.io.read_enable.poke(true.B)
        dut.io.address.poke(0.U)
        dut.clock.step()
        dut.io.data_out.expect("h11111111".U)
        
        info("✓ Address 0 preserved after multiple operations")
      }
    }

    it("should correctly handle sequential writes and reads at incrementing addresses") {
      simulate(new DMEM(mem_size = 256)) { dut =>
        // Write sequential values - use numeric literal
        for (addr <- 0 until 20) {
          dut.io.write_enable.poke(true.B)
          dut.io.read_enable.poke(false.B)
          dut.io.address.poke(addr.U)
          // CORRECTED: addr * 0x10000 (numeric)
          dut.io.data_in.poke((addr * 0x10000).U)
          dut.clock.step()
        }
        
        // Read back sequentially
        for (addr <- 0 until 20) {
          dut.io.write_enable.poke(false.B)
          dut.io.read_enable.poke(true.B)
          dut.io.address.poke(addr.U)
          dut.clock.step()
          // CORRECTED: Compare with numeric value
          dut.io.data_out.expect((addr * 0x10000).U)
        }
        
        info("✓ Sequential writes and reads correct")
      }
    }

    it("should handle alternating write and read at same address") {
      simulate(new DMEM(mem_size = 256)) { dut =>
        val addr = 7
        // Use numeric literals for values that need arithmetic
        val values = Seq(0x11111111, 0x22222222, 0x33333333, 0x44444444)
        
        for (value <- values) {
          dut.io.write_enable.poke(true.B)
          dut.io.read_enable.poke(false.B)
          dut.io.address.poke(addr.U)
          dut.io.data_in.poke(value.U)  // Int to UInt is fine
          dut.clock.step()
          
          dut.io.write_enable.poke(false.B)
          dut.io.read_enable.poke(true.B)
          dut.io.address.poke(addr.U)
          dut.clock.step()
          dut.io.data_out.expect(value.U)
        }
        
        info("✓ Alternating write/read works correctly")
      }
    }

    // Additional corrected test using the helper function
    it("should work with hex string helper function") {
      simulate(new DMEM(mem_size = 256)) { dut =>
        val hexPattern = "h01010101"
        val numericPattern = h2i(hexPattern)  // Convert to Int: 0x01010101
        
        for (addr <- 0 until 10) {
          dut.io.write_enable.poke(true.B)
          dut.io.address.poke(addr.U)
          // Use the numeric pattern for multiplication
          dut.io.data_in.poke((addr * numericPattern).U)
          dut.clock.step()
        }
        
        // Verify
        for (addr <- 0 until 10) {
          dut.io.write_enable.poke(false.B)
          dut.io.read_enable.poke(true.B)
          dut.io.address.poke(addr.U)
          dut.clock.step()
          dut.io.data_out.expect((addr * numericPattern).U)
        }
        
        info("✓ Helper function works correctly")
      }
    }

  }
}