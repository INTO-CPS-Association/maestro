//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//

#ifndef BOOLEANLOGIC_H
#define BOOLEANLOGIC_H


class BooleanLogicImpl {

    bool allTrue(bool booleanValue){return false;}
    bool allFalse(bool booleanValue){return false;}

    void close(){}
};





#define BooleanLogic BooleanLogicImpl*


BooleanLogic load_BooleanLogic();
#endif //BOOLEANLOGIC_H
