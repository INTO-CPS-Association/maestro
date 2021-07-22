//
// Created by Kenneth Guldbrandt Lausdahl on 21/05/2021.
//

#include "unzip.h"

/* standard library headers */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* for POSIX mkdir, open, etc */
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

/* zip file handling */
#include <zip.h>

#include <cassert>

/* constants */
#define ERR_SIZE 256
#define COPY_BUF_SIZE 2048

#ifdef _WIN32
//windows does not support mode
int mkdir(const char *path, mode_t mode){
    return mkdir(path);
}
#endif

void inline configure_path_buffer(char *dest, const char *extract_to, const char *path) {
    if (extract_to) {
        strcpy(dest, extract_to);
        strcat(dest, "/");
        strcat(dest, path);
    } else {
        strcpy(dest, path);
    }
}

int mkpath(char *file_path, mode_t mode) {
    assert(file_path && *file_path);
    for (char *p = strchr(file_path + 1, '/'); p; p = strchr(p + 1, '/')) {
        *p = '\0';
        if (mkdir(file_path, mode) == -1) {
            if (errno != EEXIST) {
                *p = '/';
                return -1;
            }
        }
        *p = '/';
    }
    return 0;
}

bool unzip(const char *path, const char *dest) {
    /* our program's state */
    struct zip *p_zip = NULL;       /* zip archive state */
    zip_int64_t n_entries;          /* number of entries in archive */
    struct zip_file *p_file = NULL; /* state for a file within zip archive */
    int file_fd = -1, bytes_read;   /* state used when extracting file */
    char copy_buf[COPY_BUF_SIZE];   /* temporary buffer for file contents */

    /* options */
    const char *password;

    /* libzip error handling and reporting (actually only used for zip_open in
     * this case) */
    int error;
    char errstr[ERR_SIZE];

    /* handle command-line arguments */
//    if(argc < 2) {
//        fprintf(stderr, "syntax: %s <zipfile> [password]\n", argv[0]);
//        goto bail;
//    }

    /* extract password from command-line options if specified */
    password = NULL;// (argc >= 3) ? argv[2] : NULL;

    /* open the archive */
    p_zip = zip_open(path, 0, &error);
    if (p_zip == NULL) {
        zip_error_to_str(errstr, ERR_SIZE, error, errno);
        fprintf(stderr, "%s: %s\n", path, errstr);
        goto bail;
    }

    /* set (or, perhaps, un-set) the default password for files */
    if (zip_set_default_password(p_zip, password)) {
        fprintf(stderr, "error setting default password: %s\n", zip_strerror(p_zip));
    }

    /* for each entry... */
    n_entries = zip_get_num_entries(p_zip, 0);
    for (zip_int64_t entry_idx = 0; entry_idx < n_entries; entry_idx++) {
        /* FIXME: we ignore mtime for this example. A stricter implementation may
         * not do so. */
        struct zip_stat file_stat;

        /* get file information */
        if (zip_stat_index(p_zip, entry_idx, 0, &file_stat)) {
            fprintf(stderr, "error stat-ing file at index %i: %s\n",
                    (int) (entry_idx), zip_strerror(p_zip));
            goto bail;
        }

        /* check which fields are valid */
        if (!(file_stat.valid & ZIP_STAT_NAME)) {
            fprintf(stderr, "warning: skipping entry at index %i with invalid name.\n",
                    (int) entry_idx);
            continue;
        }


        char filePathBuffer[strlen(file_stat.name) + (dest ? strlen(dest) : 0) + 1];
        configure_path_buffer(filePathBuffer, dest, file_stat.name);

        /* show the user what we're doing */
        // printf("extracting: %s\n", filePathBuffer);

        /* is this a directory? */
        if ((file_stat.name[0] != '\0') && (file_stat.name[strlen(file_stat.name) - 1] == '/')) {
            /* yes, create it noting that it isn't an error if the directory already exists */
            if (mkpath(filePathBuffer, 0777) && (errno != EEXIST) && mkdir(filePathBuffer, 0777) && (errno != EEXIST)) {
                perror("error creating directory");
                goto bail;
            }

            /* loop to the next file */
            continue;
        }

        /* the file is not a directory if we get here */
        //but it could be in one that was not created yet so lets check that
        const char *slashIndex = strrchr(file_stat.name, '/');
        if (slashIndex) {
            //ok the file is in a directory
            if (mkpath(filePathBuffer, 0777) && (errno != EEXIST)) {
                perror("error creating directory");
                goto bail;
            }
        }


        /* try to open the file in the filesystem for writing */
        //  printf("##Extracting '%s'\n",filePathBuffer);
        #ifdef _WIN32
        if ((file_fd = open(filePathBuffer, O_CREAT | O_TRUNC | O_WRONLY | _O_BINARY, 0666)) == -1) {
        #else
        if ((file_fd = open(filePathBuffer, O_CREAT | O_TRUNC | O_WRONLY, 0666)) == -1) {
        #endif
            perror("cannot open file for writing");
            goto bail;
        }

        /* open the file in the archive */
        if ((p_file = zip_fopen_index(p_zip, entry_idx, 0)) == NULL) {
            fprintf(stderr, "error extracting file: %s\n", zip_strerror(p_zip));
            goto bail;
        }

        /* extract file */
        do {
            /* read some bytes */

            if ((bytes_read = zip_fread(p_file, copy_buf, COPY_BUF_SIZE)) == -1) {
                fprintf(stderr, "error extracting file: %s\n", zip_strerror(p_zip));
                goto bail;
            }

            /* if some bytes were read... */
            if (bytes_read > 0) {
                write(file_fd, copy_buf, bytes_read);
            }

            /* loop until we read no more bytes */
        } while (bytes_read > 0);

        /* close file in archive */
        zip_fclose(p_file);
        p_file = NULL;

        /* close file in filesystem */
        close(file_fd);
        file_fd = -1;
    }

    /* close the archive */
    if (zip_close(p_zip)) {
        fprintf(stderr, "error closing archive: %s\n", zip_strerror(p_zip));
    }

    /* return back to OS */
    return EXIT_SUCCESS;

    bail:
    /* jumped to if something goes wrong */

    /* close any output file */
    if (file_fd != -1) {
        close(file_fd);
        file_fd = -1;
    }

    /* close any archive file */
    if (p_file) {
        zip_fclose(p_file);
        p_file = NULL;
    }

    /* close the zip file if it was opened */
    if (p_zip)
        zip_close(p_zip);

    return false;
}