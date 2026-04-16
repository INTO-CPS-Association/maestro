package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.*;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.DoubleExpressionValue;
import org.intocps.maestro.framework.fmi2.api.mabl.values.IntExpressionValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.call;
import static org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api.PortFilters.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class InstanceClocksFmi3 {
    static final double FALSE = 0.0;
    static final double TRUE = 1.0;
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

    public void deactivate(PortFmi3Api clock) {
        getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Triggered)).setValue(DoubleExpressionValue.of(FALSE));
    }

    enum ClockInfo {
        Shift(0),
        Interval(1),
        Ticked(2),
        Triggered(3),
        LastTickTime(4);
        final int value;

        ClockInfo(int value) {
            this.value = value;
        }
    }

    public VariableFmi2Api getClockTriggeredState(PortFmi3Api clock) {
        return getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Triggered));
    }

    public PredicateFmi2Api check(FmiBuilder.Scope<PStm> scope, DoubleExpressionValue time, PortFmi3Api clock) {

        var lastTickTime = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.LastTickTime));
        var ticked = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Ticked));
        var shift = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift));
        var interval = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval));
        var triggered = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Triggered));

        var ifTicked = scope.enterIf(shift.toMath().greaterEqualTo(IntExpressionValue.of(0)).and(ticked.toMath().equalTo(DoubleExpressionValue.of(FALSE))));
        {
            /* the clock never ticked before*/
            var ifTriggered = scope.enterIf(time.greaterEqualTo(shift.toMath()));
            triggered.setValue(DoubleExpressionValue.of(TRUE));
            lastTickTime.setValue(time);
            ticked.setValue(DoubleExpressionValue.of(TRUE));//we will never come back
//            ifTriggered.enterElse();
//            triggered.setValue(DoubleExpressionValue.of(FALSE));
            ifTicked.leave();
        }
        ifTicked.enterElse();
        {
            /* the clock have ticked before so we check for interval on time*/
            var ifTriggered = scope.enterIf(time.greaterEqualTo(lastTickTime.toMath().addition(interval.toMath())));
            triggered.setValue(DoubleExpressionValue.of(TRUE));
            lastTickTime.setValue(time);
            ifTriggered.enterElse();
            triggered.setValue(DoubleExpressionValue.of(FALSE));
            ifTriggered.leave();
        }
        ifTicked.leave();
        return triggered.toMath().equalTo(DoubleExpressionValue.of(TRUE));
    }

    public DoubleVariableFmi2Api getTimeToTick(DoubleExpressionValue now, PortFmi3Api clock) {
        var lastTickTime = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.LastTickTime));
        var ticked = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Ticked));
        var shift = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift));
        var interval = getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval));
        var scope = builder.getDynamicScope();

        NumericExpressionValueFmi2Api tickTime = lastTickTime.toMath().addition(interval.toMath());
        builder.getLogger().trace("Interval %f", getInterval(clock));
        var vd = new DoubleVariableFmi2Api(null, null, null, null, tickTime.subtraction(now).getExp());
        builder.getLogger().trace("Time diff %f", vd);
//        var v = new DoubleVariableFmi2Api(null, null, null, null, tickTime.subtraction(now).getExp());
        var timeToTickVar = scope.store("time2Tick", vd);
        var ifTicked = scope.enterIf(ticked.toMath().equalTo(DoubleExpressionValue.of(FALSE)));
        {
            var v1 = new DoubleVariableFmi2Api(null, null, null, null, now.subtraction(lastTickTime.toMath()).addition(shift.toMath()).getExp());
            // timeToTickVar.setValue(v1);


        }

        ifTicked.leave();
        return new DoubleVariableFmi2Api(null, null, null, null, timeToTickVar.getExp());
    }

    private int getClockIndex(PortFmi3Api clock, ClockInfo type) {
        return this.clocks.indexOf(clock) * ClockInfo.values().length + type.value;
    }

    public void storeInterval(FmiBuilder.Scope<PStm> scope, PortFmi3Api clock, PExp interval) {
        scope
                .add(newAAssignmentStm(getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval)).getDesignator(), interval));
    }

    public VariableFmi2Api<DoubleVariableFmi2Api> getInterval(PortFmi3Api clock) {
        return getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Interval));
    }

    public void storeShift(FmiBuilder.Scope<PStm> scope, PortFmi3Api clock, PExp shift) {
        scope
                .add(newAAssignmentStm(getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift)).getDesignator(), shift));
    }

    public VariableFmi2Api<DoubleVariableFmi2Api> getShift(PortFmi3Api clock) {
        return getClockTimingVar().items().get(getClockIndex(clock, ClockInfo.Shift));
    }

    private ArrayVariableFmi2Api<DoubleVariableFmi2Api> getClockTimingVar() {

        if (this.timingDataStorage == null) {
            final int length = this.clocks.size() * ClockInfo.values().length;
            final PType type = new ARealNumericPrimitiveType();
            timingDataStorage = createDataArray(length, "clocks_timing", type);
        }
        return this.timingDataStorage;
    }

    private PStm getOrCreate(PStm var) {

        if (builder instanceof ExpansionMableApiBuilder expBuilder && var instanceof ALocalVariableStm stm) {
            var decl = expBuilder.findDecleration(this.owner.getDeclaredScope(), stm.getDeclaration().getName().getText());
            if (decl != null) {
                return decl.getValue();
            }
        }

        this.owner.getDeclaredScope().addAfter(this.owner.getDeclaringStm(), var);
        return var;
    }

    @SuppressWarnings("deprecation")
    private <C extends VariableFmi2Api> ArrayVariableFmi2Api<C> createDataArray(int length, String name, PType type) {

        String ioBufName = this.owner.getName() + name;// builder.getNameGenerator().getName(this.owner.getName(), name);

        PStm var = getOrCreate(newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length,
                new AArrayInitializer(Stream.generate(() -> (PExp) new AIntLiteralExp(null, 0)).limit(length).toList()))));

//        this.owner.getDeclaredScope().addAfter(this.owner.getDeclaringStm(), var);

        List<VariableFmi2Api> items;
        if (name.equals("clocks_timing")) {
            //for better readability we need to index with names
            var indexedNames = Arrays.stream(ClockInfo.values()).sorted(Comparator.comparingInt(a -> a.value)).toList();
            var index2Name = indexedNames.stream()
                    .collect(Collectors.toMap(Function.identity(), index -> index.name().toUpperCase()));

            indexedNames.forEach(index -> {
                var indexVar = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(index2Name.get(index)), newAIntNumericPrimitiveType(),
                        new AExpInitializer(new AIntLiteralExp(null, index.value))));
//                this.owner.getDeclaredScope().addAfter(this.owner.getDeclaringStm(), indexVar);
                getOrCreate(indexVar);
            });
            items = indexedNames.stream().map(index -> {

                var indexExp = new AIdentifierExp(null, new LexIdentifier(index2Name.get(index), null));

                return new VariableFmi2Api<>(var, type, this.owner.getDeclaredScope(), builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), indexExp.clone()),
                        newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(indexExp.clone())));

            }).collect(Collectors.toList());
        } else {
            items = IntStream.range(0, length)
                    .mapToObj(i -> new VariableFmi2Api<>(var, type, this.owner.getDeclaredScope(), builder.getDynamicScope(),
                            newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                            newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());
        }
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

        this.owner.handleError(scope, new InstanceVariableFmi3Api.CallContext("getIntervalDecimal", args));

        for (short i = 0; i < selectedPorts.size(); i++) {
            this.storeInterval(scope, selectedPorts.get(i), intervalBuf.items().get(i).getReferenceExp().clone());
        }
    }


    public void updateClockShifts(FmiBuilder.Scope<PStm> scope, ArrayVariableFmi2Api<Object> vrefBuf,
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


        var shiftBuffer1 = getShiftBuffer();


//        int getShiftDecimal(uint valueReferences[], int nValueReferences, real shifts[]);

        List<PExp> args = new ArrayList<>(
                List.of(vrefBuf.getReferenceExp().clone(), newAUIntLiteralExp((long) selectedPorts.size()),
                        newARefExp(shiftBuffer1.getReferenceExp().clone())));
        AAssigmentStm stm = newAAssignmentStm(((IMablScope) scope).getFmiStatusVariable().getDesignator().clone(),
                call(this.owner.getReferenceExp().clone(), "getShiftDecimal", args));
        scope.add(stm);

        this.owner.handleError(scope, new InstanceVariableFmi3Api.CallContext("getShiftDecimal", args));

        for (short i = 0; i < selectedPorts.size(); i++) {
            this.storeShift(scope, selectedPorts.get(i), shiftBuffer1.items().get(i).getReferenceExp().clone());
        }
    }
}
