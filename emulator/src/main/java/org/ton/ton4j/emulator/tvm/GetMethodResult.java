package org.ton.ton4j.emulator.tvm;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.*;

@Builder
@Data
public class GetMethodResult implements Serializable {
  boolean success;
  String error;
  String vm_log;
  int vm_exit_code;
  @ToString.Exclude String stack; // Base64 encoded BoC serialized stack (VmStack)
  String missing_library;
  int gas_used;

  @ToString.Include(name = "stackBase64")
  public String getStackBase64() {
    return StringUtils.isNotEmpty(stack) ? stack : "";
  }

  public VmStack getStack() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack)));
    }
    return VmStack.builder().build();
  }

  public Cell getStackCell() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack))).toCell();
    }
    return VmStack.builder().build().toCell();
  }

  public Cell getStackFirstEntrySlice() {
    if (StringUtils.isNotEmpty(stack)) {
      return VmCellSlice.deserialize(
              CellSlice.beginParse(
                  VmStack.deserialize(CellSlice.beginParse(Cell.fromBocBase64(stack)))
                      .getStack()
                      .getTos()
                      .get(0)
                      .toCell()))
          .getCell();
    }
    return VmCellSlice.builder().build().toCell();
  }

  public BigInteger getIntByIndex(int stackIndex) {
    VmStack vmStack = getStack();
    VmStackValue vmStackValue = vmStack.getStack().getTos().get(stackIndex);
    if (vmStackValue instanceof VmStackValueInt) {
      return ((VmStackValueInt) vmStack.getStack().getTos().get(stackIndex)).getValue();
    } else if (vmStackValue instanceof VmStackValueTinyInt) {
      return ((VmStackValueTinyInt) vmStack.getStack().getTos().get(stackIndex)).getValue();
    } else {
      throw new RuntimeException(
              "Unsupported vm stack value type: " + vmStackValue + ". Expecting number.");
    }
  }

  public Cell getCellByIndex(int stackIndex) {
    return ((VmStackValueCell) getStack().getStack().getTos().get(stackIndex)).getCell();
  }

  public VmTuple getTupleByIndex(int stackIndex) {
    return (VmTuple) getStack().getStack().getTos().get(stackIndex);
  }

  public List<VmStackValue> getListByIndex(int stackIndex) {
    try {
      return ((VmStackList) getStack().getStack().getTos().get(stackIndex)).getTos();

    } catch (Throwable e) {
      return new ArrayList<>();
    }
  }

  public VmCellSlice getSliceByIndex(int stackIndex) {
    return ((VmStackValueSlice) getStack().getStack().getTos().get(stackIndex)).getCell();
  }

  public Address getAddressByIndex(int stackIndex) {
    Cell cell = ((VmStackValueCell) getStack().getStack().getTos().get(stackIndex)).getCell();
    return CellSlice.beginParse(cell).loadAddress();
  }
}
