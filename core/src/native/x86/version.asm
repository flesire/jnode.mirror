; -----------------------------------------------
; $Id: version.asm 1523 2005-05-06 07:49:40Z epr $
;
; JNode Version
;
; Author       : E.Prangsma 
; -----------------------------------------------

global sys_version
; sys_version: db '$Id: version.asm 1523 2005-05-06 07:49:40Z epr $',0xd,0xa,0

sys_version: db 'Version ',JNODE_VERSION,0xd,0xa,0
