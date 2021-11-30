     .global decoder
     .global decoder_size
     .section .rodata
 decoder:
 #if defined __aarch64__
     .incbin "/storage/emulated/0/AppProjects/Silk解码器/app/src/main/cpp/decoder"
 #endif
 1:
 decoder_size:
     .int 1b - decoder
