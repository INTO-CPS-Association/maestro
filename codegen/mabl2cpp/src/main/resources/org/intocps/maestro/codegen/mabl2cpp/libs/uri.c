/*
 *
 * Shared Memory FMI
 * 
 * Copyright (C) 2015 - 2017 Overture
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 *
 * Author: Kenneth Lausdahl
 */


#include "uri.h"

#if defined WIN32 || defined WIN64
static const char native_path_separator = '\\';
static const char foreign_path_separator = '/';
#else
static const char native_path_separator = '/';
static const char foreign_path_separator = '\\';
#endif

/**
 * Convert an uri provided by the the co-simulator to a native path
 * @param uri Input path. native and file:/ and file:/// uri's right now
 * @return Natve path that ends with a path separator.
 * Note that the caller is responsible for free-ing the allocated string buffer
 * memory
 */
const char* URIToNativePath(const char* uri) {
  unsigned int path_start = 0;
  char* path = NULL;
  unsigned int path_len = 0;
  unsigned int uri_len = 0;
  char* pch = NULL;
  unsigned int i = 0;
  unsigned int j = 0;
  char buf[3] = "00";

  if (!uri) {
    return NULL;
  }

  uri_len = (unsigned int)strlen(uri);

  if (uri_len == 0) {
    return NULL;
  }

  /* Check if we got a file:/// uri */
  if (strncmp(uri, "file:///", 8) == 0) {
    if (uri[9] == ':') {
      // Windows drive letter in the URI (e.g. file:///c:/ uri
      /* Remove the file:/// */
      path_start = 8;
    } else {
      /* Remove the file:// but keep the third / */
      path_start = 7;
    }
  }
#if defined WIN32 || defined WIN64
  /* Check if we got a file://hostname/path uri */
  else if (strncmp(uri, "file://", 7) == 0) {
    /* Convert to a network share path: //hostname/path */
    path_start = 5;
  }
#endif
  /* Check if we got a file:/ uri */
  else if (strncmp(uri, "file:/", 6) == 0) {
    if (uri[7] == ':') {
      // Windows drive letter in the URI (e.g. file:/c:/ uri
      /* Remove the  file:/ */
      path_start = 6;
    } else {
      /* Remove the file: but keep the / */
      path_start = 5;
    }
  }
  /* Assume that it is a native path */
  else {
    path_start = 0;
  }

  /* Check the length of the remaining string */
  path_len = (int)strlen(&uri[path_start]);
  if (path_len == 0) {
    return NULL;
  }

  path = (char*)malloc(path_len + 1);

  /* Copy the remainder of the uri and replace all percent encoded character
  * by their ASCII character and translate slashes to backslashes on Windows
  * and backslashes to slashes on other OSses
  */
  for (i = path_start, j = 0; i < uri_len; i++, j++) {
    if (uri[i] == '%') {
      /* Replace the precent-encoded hexadecimal digits by its US-ASCII
      * representation */
      if (i < uri_len - 2) {
        if ((isxdigit(uri[i + 1])) && (isxdigit(uri[i + 2]))) {
          strncpy(buf, uri + i + 1, 2);
          path[j] = (unsigned char)strtol(buf, NULL, 16);
          i += 2;
          path_len -= 2;
        } else {
          /* Not percent encoded, keep the % */
          path[j] = uri[i];
        }
      } else {
        /* Not percent encoded, keep the % */
        path[j] = uri[i];
      }
    } else if (uri[i] == foreign_path_separator) {
      /* Translate slashes to backslashes on Windows and backslashes to slashes
       * on other OSses */
      path[j] = native_path_separator;
    } else {
      /* Just copy the character */
      path[j] = uri[i];
    }
  }

  /* Make sure that the string is always NULL terminated */
  path[path_len ] = '\0';

  return path;
}
