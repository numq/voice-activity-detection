#ifndef VAD_DELETER_H
#define VAD_DELETER_H

#include "fvad.h"

struct fvad_deleter {
    void operator()(Fvad *fvad) { fvad_free(fvad); }
};

typedef std::unique_ptr <Fvad, fvad_deleter> fvad_ptr;

#endif //VAD_DELETER_H
