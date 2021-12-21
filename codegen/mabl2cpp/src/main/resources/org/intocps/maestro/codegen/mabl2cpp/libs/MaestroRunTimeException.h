//
// Created by Kenneth Guldbrandt Lausdahl on 21/12/2021.
//

#ifndef SIM_MAESTRORUNTIMEEXCEPTION_H
#define SIM_MAESTRORUNTIMEEXCEPTION_H

#include <exception>

struct MaestroRunTimeException : public std::exception {

private:
    const char *msg;
public:
    explicit MaestroRunTimeException(const char *msg) : msg(msg) {}

    [[nodiscard]] const char *what() const noexcept override {
        return msg;
    }
};

#endif //SIM_MAESTRORUNTIMEEXCEPTION_H
