//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//

#ifndef ZLIB_DOWNLOAD_BOOLEANLOGIC_H
#define ZLIB_DOWNLOAD_BOOLEANLOGIC_H


class BooleanLogicImpl {

    bool allTrue(bool booleanValue){return false;}
    bool allFalse(bool booleanValue){return false;}

    void close(){}
};





#define BooleanLogic BooleanLogicImpl*


BooleanLogic load_BooleanLogic();
#endif //ZLIB_DOWNLOAD_BOOLEANLOGIC_H
