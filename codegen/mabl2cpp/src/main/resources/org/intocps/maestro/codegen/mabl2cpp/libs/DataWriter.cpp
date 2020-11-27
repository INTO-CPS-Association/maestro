#include "DataWriter.h"


DataWriter load_DataWriter(){
    return new DataWriterImpl();
}



void DataWriterImpl::close() {
      myfile.close();

}
void * DataWriterImpl::writeHeader( int size,std::string * headers) {


     myfile.open ("outputs.csv");
myfile<< "\"time\",";
     for(int i = 0; i<size; i++)
     {
     myfile <<"\""<< headers[i] <<"\"";
     if(i+1 < size)
      myfile <<" , ";
     }

myfile << std::endl;
return nullptr;

}

void DataWriterImpl::writeDataPoint(const char * fmt,void *,  double time, ...) {
    va_list args;
    va_start(args, time);

//skip first
va_arg(args, int);
//    const char* fmt = "df";
    myfile <<time << " , ";
bool first = true;
    while (*fmt != '\0') {

    if(!first && (*fmt == 'i' || *fmt=='b'|| *fmt=='r'))
    {
        myfile <<  " , ";
    }
    first = false;


        if (*fmt == 'i') {
            int i = va_arg(args, int);
            myfile << i ;
        }else if( *fmt=='b'){
         int i = va_arg(args, int);
         if(i==1)
                    myfile << "true";
                    else
               myfile<<"false" ;
        } else if (*fmt == 's') {
            // note automatic conversion to integral type
            int c = va_arg(args, int);
           myfile << static_cast<char>(c);
        } else if (*fmt == 'r') {
            double d = va_arg(args, double);
            myfile << d ;
        }
        ++fmt;
    }


    va_end(args);
    myfile << std::endl;
}