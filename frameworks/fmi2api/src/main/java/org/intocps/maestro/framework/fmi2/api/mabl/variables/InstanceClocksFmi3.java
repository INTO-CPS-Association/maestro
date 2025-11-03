package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api.PortFilters.*;

public class InstanceClocksFmi3 {

    private final InstanceVariableFmi3Api owner;
    private final List<PortFmi3Api> clocks;
    private final MablApiBuilder builder;

    /**
     * Data stored in the format:
     * array[clockindex] = shift
     * array[clockindex+1] = interval
     */
    ArrayVariableFmi2Api<DoubleVariableFmi2Api> timingDataStorage;

    ArrayVariableFmi2Api<DoubleVariableFmi2Api> intervalBuffer = null;
    ArrayVariableFmi2Api<IntVariableFmi2Api> intervalQualifierBuffer = null;
    ArrayVariableFmi2Api<DoubleVariableFmi2Api> shiftBuffer = null;


    public ArrayVariableFmi2Api<IntVariableFmi2Api> getIntervalQualifierBuffer() {

        if (this.intervalQualifierBuffer == null) {
            final int length = this.clocks.size();
            final PType type = new AIntNumericPrimitiveType();
            intervalQualifierBuffer = createDataArray(length, "clocks_timing_interval_qualifier_buffer", type);
        }
        return this.intervalQualifierBuffer;
    }

    public ArrayVariableFmi2Api<DoubleVariableFmi2Api> getIntervalBuffer() {

        if (this.intervalBuffer == null) {
            final int length = this.clocks.size();
            final PType type = new ARealNumericPrimitiveType();
            intervalBuffer = createDataArray(length, "clocks_timing_interval_buffer", type);
        }
        return this.intervalBuffer;
    }

    public ArrayVariableFmi2Api<DoubleVariableFmi2Api> getShiftBuffer() {

        if (this.shiftBuffer == null) {
            final int length = this.clocks.size();
            final PType type = new ARealNumericPrimitiveType();
            shiftBuffer = createDataArray(length, "clocks_timing_shift_buffer", type);
        }
        return this.shiftBuffer;
    }


    public InstanceClocksFmi3(MablApiBuilder builder, InstanceVariableFmi3Api instance) {
        this.owner = instance;

        this.builder = builder;
        this.clocks = instance.getPorts().stream().filter(isClockTimeBased).filter(isCausalityInput).toList();
    }

    enum ClockInfo {
        Shift,
        Interval
    }

    private int getClockIndex(PortFmi3Api clock, ClockInfo type) {
        switch (type) {
            case Shift -> {
                return this.clocks.indexOf(clock) * 2 + 1;
            }
            case Interval -> {
                return this.clocks.indexOf(clock) * 2;
            }
        }
        throw new RuntimeException("unknown clock info type: " + type);
    }

    public void storeInterval(FmiBuilder.Scope<PStm> scope, PortFmi3Api clock, PExp interval) {
        scope
                .add(newAAssignmentStm(getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval)).getDesignator(), interval));
    }

    public PExp getInterval(PortFmi3Api clock) {
        return getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval)).getReferenceExp();
    }

    public void storeShift(FmiBuilder.Scope<PStm> scope, PortFmi3Api clock, PExp shift) {
        scope
                .add(newAAssignmentStm(getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift)).getDesignator(), shift));
    }

    public PExp getShift(PortFmi3Api clock) {
        return getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift)).getReferenceExp();
    }

    private ArrayVariableFmi2Api<DoubleVariableFmi2Api> getClockTimingVar() {

        if (this.timingDataStorage == null) {
            final int length = this.clocks.size() * 2;
            final PType type = new ARealNumericPrimitiveType();
            timingDataStorage = createDataArray(length, "clocks_timing_data", type);
        }
        return this.timingDataStorage;
    }

    private <C extends VariableFmi2Api> ArrayVariableFmi2Api<C> createDataArray(int length, String name, PType type) {

        String ioBufName = builder.getNameGenerator().getName(this.owner.getName(), name);

        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length, null));

        this.owner.getDeclaredScope().addAfter(this.owner.getDeclaringStm(), var);

        List<VariableFmi2Api> items = IntStream.range(0, length)
                .mapToObj(i -> new VariableFmi2Api<>(var, type, this.owner.getDeclaredScope(), builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

        return new ArrayVariableFmi2Api(var, type, this.owner.getDeclaredScope(), builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName), items.stream().collect(Collectors.toList()));
    }

    public void updateClockIntervals(FmiBuilder.Scope<PStm> scope, ArrayVariableFmi2Api<Object> vrefBuf,
                                     FmiBuilder.Port<Fmi3ModelDescription.Fmi3ScalarVariable, PStm>... ports) {

        List<PortFmi3Api> selectedPorts;
        if (ports == null || ports.length == 0) {
            return;// Map.of();
        } else {
            selectedPorts = Arrays.stream(ports).map(PortFmi3Api.class::cast).filter(isClock).filter(isCausalityInput).toList();
        }

        for (int i = 0; i < selectedPorts.size(); i++) {
            PortFmi3Api p = selectedPorts.get(i);
            PStateDesignator designator = vrefBuf.items().get(i).getDesignator().clone();
            scope.add(newAAssignmentStm(designator, newAIntLiteralExp(p.getPortReferenceValue().intValue())));
        }


        var intervalBuf = getIntervalBuffer();
        var qualifierBuf = getIntervalQualifierBuffer();


//        int getIntervalDecimal(uint valueReferences[], int nValueReferences, real intervals[],
//        int qualifiers[]);

        List<PExp> args = new ArrayList<>(
                List.of(vrefBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) selectedPorts.size()), newARefExp(intervalBuf.getReferenceExp().clone()),
                        newARefExp(qualifierBuf.getReferenceExp().clone())));
        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.owner.getReferenceExp().clone(), "getIntervalDecimal", args));
        scope.add(stm);

           this.owner. handleError(scope, new InstanceVariableFmi3Api.CallContext("getIntervalDecimal", args));

        for (short i = 0; i < selectedPorts.size(); i++) {
            this.storeInterval(scope, selectedPorts.get(i), intervalBuf.items().get(i).getReferenceExp().clone());
        }
    }
}
