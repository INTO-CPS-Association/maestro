portsInSameState([], [], _, _).
portsInSameState([port(N,_) | PP], [pstate(N, Defined, TimeStamp)|PStates], Defined, TimeStamp):-
  portsInSameState(PP, PStates, Defined, TimeStamp).

inState([], [], _, _).
inState([fmu(Name,Inports,OutPorts)|FF], [fstate(Name, TStamp, InportsState, OutportsState)|FFState], DefinedPort, TStamp):-
  portsInSameState(Inports, InportsState, DefinedPort, TStamp),
  portsInSameState(OutPorts, OutportsState, DefinedPort, TStamp),
  inState(FF, FFState, DefinedPort, TStamp).

% Checks whether a list of names have a given state.
namesInState([], _, _, _).
namesInState([Name | NN], State, Defined, TimeStamp):-
  nameInState(Name, State, Defined, TimeStamp),
  namesInState(NN, State, Defined, TimeStamp).

nameInState(Name, [fstate(Name, TimeStamp, _, _)|_], _, TimeStamp).
nameInState(Name, [fstate(_, _, InportsState, OutportsState)|FFState], Defined, TimeStamp):-
  nameInState(Name, InportsState, Defined, TimeStamp)
  ; nameInState(Name, OutportsState, Defined, TimeStamp)
  ; nameInState(Name, FFState, Defined, TimeStamp).
nameInState(Name, [pstate(Name, Defined, TimeStamp)|_], Defined, TimeStamp).
nameInState(Name, [pstate(_, _, _)|PPState], Defined, TimeStamp):-
  nameInState(Name, PPState, Defined, TimeStamp).

portsInSameTimeStamp([], _ , _).
portsInSameTimeStamp([port(N,_) | PP], [pstate(N, _, TimeStamp)|PStates], TimeStamp):-
  portsInSameTimeStamp(PP, PStates, TimeStamp).

% Defines state of a co-sim scenario at time 0
inStateT0(FMUs, StateT):-
  inState(FMUs, StateT, undefined, t).

% Defines state of a co-sim scenario at time t
inStateT(FMUs, StateT):-
  inState(FMUs, StateT, defined, t).

% Operations on State
setDefine(EName, EDefine, [fstate(Name, TimeStamp, InStates1, OutStates)|SS], [fstate(Name, TimeStamp, InStates2, OutStates)|SS]):-
  setDefine(EName, EDefine, InStates1, InStates2).
setDefine(EName, EDefine, [fstate(Name, TimeStamp, InStates, OutStates1)|SS], [fstate(Name, TimeStamp, InStates, OutStates2)|SS]):-
  setDefine(EName, EDefine, OutStates1, OutStates2).
setDefine(EName, EDefine, [fstate(Name, TimeStamp, InStates, OutStates)|SS1], [fstate(Name, TimeStamp, InStates, OutStates)|SS2]):-
  setDefine(EName, EDefine, SS1, SS2).
setDefine(EName, EDefine, [pstate(EName,_,TimeStamp)|SS], [pstate(EName, EDefine, TimeStamp)|SS]).
setDefine(EName, EDefine, [pstate(Name, Define, TimeStamp)|SS1], [pstate(Name, Define, TimeStamp)|SS2]):-
  setDefine(EName, EDefine, SS1, SS2).

setTimestamp(EName, ETimestamp, [fstate(EName, _, InStates, OutStates)|SS], [fstate(EName, ETimestamp, InStates, OutStates)|SS]).
setTimestamp(EName, ETimestamp, [fstate(Name, TimeStamp, InStates1, OutStates)|SS], [fstate(Name, TimeStamp, InStates2, OutStates)|SS]):-
  setTimestamp(EName, ETimestamp, InStates1, InStates2).
setTimestamp(EName, ETimestamp, [fstate(Name, TimeStamp, InStates, OutStates1)|SS], [fstate(Name, TimeStamp, InStates, OutStates2)|SS]):-
  setTimestamp(EName, ETimestamp, OutStates1, OutStates2).
setTimestamp(EName, ETimestamp, [fstate(Name, TimeStamp, InStates, OutStates)|SS1], [fstate(Name, TimeStamp, InStates, OutStates)|SS2]):-
  setTimestamp(EName, ETimestamp, SS1, SS2).
setTimestamp(EName, ETimestamp, [pstate(EName, Define, _)|SS], [pstate(EName, Define, ETimestamp)|SS]).
setTimestamp(EName, ETimestamp, [pstate(Name, Define, TimeStamp)|SS1], [pstate(Name, Define, TimeStamp)|SS2]):-
  setTimestamp(EName, ETimestamp, SS1, SS2).

% Operations on the State of FMUs
setPortsDefine([], _, State, State).
setPortsDefine([port(N,_) | PP], Define, StateBefore, StateAfter):-
  setPortsDefine(PP, Define, StateBefore, SAUX),
  setDefine(N, Define, SAUX, StateAfter).

setPortsTimestamp([], _, State, State).
setPortsTimestamp([port(N,_) | PP], Timestamp, StateBefore, StateAfter):-
  setPortsTimestamp(PP, Timestamp, StateBefore, SAUX),
  setTimestamp(N, Timestamp, SAUX, StateAfter).

% Constraints on the State of FMUs according to capabilities
checkInputContract([], _).
checkInputContract([port(I, delayed)| IPs], PortState):-
    member(pstate(I, defined, t), PortState),
    checkInputContract(IPs, PortState).
checkInputContract([port(I, reactive)| IPs], PortState):-
    member(pstate(I, defined, tH), PortState),
    checkInputContract(IPs, PortState).

outPortInState(F, P, D, T, State):-
  member(fstate(F, _, _, OutStates), State),
  member(pstate(P, D, T), OutStates).

connectionsState(FI, I, Connections, D, T, State):-
  member(connect(FO, O, FI, I), Connections),
  outPortInState(FO, O, D, T, State),
  allConnectionsInState(FI, I, Connections, D, T, State).

allConnectionsInState(_, _, [], _, _, _).
allConnectionsInState(FI, I, [connect(FO,O,FI,I)|Connections], D, T, State):-
  outPortInState(FO, O, D, T, State),
  allConnectionsInState(FI, I, Connections, D, T, State).
allConnectionsInState(FI, I, [connect(_,_,_,_)|Connections], D, T, State):-
    allConnectionsInState(FI, I, Connections, D, T, State).

executeOp(getOut(F, O), FMUs, _,
              StateBefore, StateAfter):-
  member(fmu(F, _, Outports), FMUs),
  member(port(O, Deps), Outports),
  member(
    fstate(F, T, InStatesBefore, OutStatesBefore),
    StateBefore),
  member(pstate(O, _, T), OutStatesBefore),
  namesInState(Deps, InStatesBefore, defined, T),
  setDefine(O, defined, StateBefore, StateAfter).
executeOp(setIn(F,I), FMUs, Connections,
              StateBefore, StateAfter):-
  member(fmu(F, Inports, _), FMUs),
  member(port(I, _), Inports),
  member(
    fstate(F, _, InStatesBefore, _),
    StateBefore),
  member(pstate(I, _, _), InStatesBefore),
  connectionsState(F, I, Connections, defined, T,
                    StateBefore),
  setDefine(I, defined, StateBefore, SAUX),
  setTimestamp(I, T, SAUX, StateAfter).
executeOp(doStep(F), FMUs, _,
      StateBefore, StateAfter):-
  member(fmu(F, Inports, Outports), FMUs),
  member(
    fstate(F, t, InStatesBefore, OutStatesBefore),
    StateBefore),
  checkInputContract(Inports, InStatesBefore),
  portsInSameTimeStamp(Outports, OutStatesBefore, t),
  setTimestamp(F, tH, StateBefore, SAUX),
  setPortsDefine(Outports, undefined, SAUX, SAUX2),
  setPortsTimestamp(Outports, tH, SAUX2, StateAfter).

executeOps(_, [], _, _, State, State).
executeOps(POps, [Op | NOps], FMUs, Conns,
              StateBef, StateAft):-
  executeOp(Op, FMUs, Conns, StateBef, SAUX),
  SAUX\==StateBef,
  executeOps([Op|POps],NOps,FMUs,Conns,SAUX,StateAft).

isInitSchedule(Init, FMUs, Connections):-
	inStateT0(FMUs, StateT0),
  inStateT(FMUs, StateT),
  executeOps([], Init, FMUs, Connections, StateT0, StateT).
