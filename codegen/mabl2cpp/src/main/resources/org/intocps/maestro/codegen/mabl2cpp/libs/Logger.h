//
// Created by Kenneth Guldbrandt Lausdahl on 18/05/2021.
//
#ifndef LOGGER_H
#define LOGGER_H


#include <iostream>
#include <stdarg.h>



class LoggerImpl{
public:
    void log(int level, const char* fmt...) // C-style "const char* fmt, ..." is also valid
    {
        int ret;

        /* Declare a va_list type variable */
        va_list myargs;

        /* Initialise the va_list variable with the ... after fmt */

        va_start(myargs, fmt);

        /* Forward the '...' to vprintf */
        ret = vprintf(fmt, myargs);

        /* Clean up the va_list */
        va_end(myargs);

//        return ret;
    }
};


#define Logger LoggerImpl*


Logger load_Logger();

#endif