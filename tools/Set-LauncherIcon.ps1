param([Parameter(Mandatory=$true)][string]$Exe, [Parameter(Mandatory=$true)][string]$Icon)
$ErrorActionPreference='Stop'
Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.InteropServices;
public static class LauncherIconResources {
  private delegate bool EnumName(IntPtr module, IntPtr type, IntPtr name, IntPtr param);
  [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern IntPtr BeginUpdateResource(string file,bool delete);
  [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool UpdateResource(IntPtr handle,IntPtr type,IntPtr name,ushort language,byte[] data,uint size);
  [DllImport("kernel32.dll",SetLastError=true)] static extern bool EndUpdateResource(IntPtr handle,bool discard);
  [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] static extern IntPtr LoadLibraryEx(string file,IntPtr fileHandle,uint flags);
  [DllImport("kernel32.dll",CharSet=CharSet.Unicode)] static extern bool EnumResourceNames(IntPtr module,IntPtr type,EnumName callback,IntPtr param);
  [DllImport("kernel32.dll")] static extern bool FreeLibrary(IntPtr module);
  public static void Apply(string file,byte[] ico) {
    var groups=new List<int>();
    IntPtr module=LoadLibraryEx(file,IntPtr.Zero,2);
    if(module!=IntPtr.Zero) {
      EnumResourceNames(module,(IntPtr)14,(m,t,n,p)=> { if(n.ToInt64()<=65535) groups.Add(n.ToInt32()); return true; },IntPtr.Zero);
      FreeLibrary(module);
    }
    IntPtr update=BeginUpdateResource(file,false);
    if(update==IntPtr.Zero)throw new Win32Exception(Marshal.GetLastWin32Error(), "Begin resources");
    try {

      int count=BitConverter.ToUInt16(ico,4);
      byte[] group=new byte[6+count*14]; Array.Copy(ico,group,6);
      for(int i=0;i<count;i++) {
        int source=6+i*16, target=6+i*14;
        int size=BitConverter.ToInt32(ico,source+8), offset=BitConverter.ToInt32(ico,source+12);
        byte[] png=new byte[size]; Array.Copy(ico,offset,png,0,size);
        ushort id=(ushort)(1001+i);
        if(!UpdateResource(update,(IntPtr)3,(IntPtr)id,0,png,(uint)size))throw new Win32Exception(Marshal.GetLastWin32Error(), "Write icon image");
        Array.Copy(ico,source,group,target,12); Array.Copy(BitConverter.GetBytes(id),0,group,target+12,2);
      }
      if(groups.Count==0)groups.Add(1);
      foreach(int groupId in groups) foreach(ushort language in new ushort[]{0,1033,2052})
        if(!UpdateResource(update,(IntPtr)14,(IntPtr)groupId,language,group,(uint)group.Length))throw new Win32Exception(Marshal.GetLastWin32Error(), "Write icon group");
      if(!EndUpdateResource(update,false))throw new Win32Exception(Marshal.GetLastWin32Error(), "Commit resources");
      update=IntPtr.Zero;
    } finally { if(update!=IntPtr.Zero)EndUpdateResource(update,true); }
  }
}
"@
[LauncherIconResources]::Apply([IO.Path]::GetFullPath($Exe),[IO.File]::ReadAllBytes([IO.Path]::GetFullPath($Icon)))
