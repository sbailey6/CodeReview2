@startuml
                
package android.app{
  class ActivityManager
  class AppGlobals
  class AppOpsManager
  interface IActivityManager
}

package android.content{
  class ClipData
  class ClipData.Item
  class ClipDescription
  class ContentProvider
  class Context
  class Intent
  class IOnPrimaryClipChangedListener
}

package android.content.pm{
  interface IPackageManager
  class PackageInfo
  abstract class PackageManager
  class UserInfo
}

package android.net{
	class Uri
}

package android.os{
	class Binder
	interface IBinder
	interface IUserManager
	class Parcel
	class Process
	class RemoteCallbackList
	class RemoteException
	class ServiceManager
	class SystemProperties
	class UserHandle
	class UserManager
}

package android.util{
	class Slog
	class SparseArray
}

package com.android.server.clipboard{
	class ClipboardService
	interface HostClipboardCallback 
	class HostClipboardMonitor
	class PerUserClipboard
	class ClipboardImpl                    
	class ListenerInfo
}

package java.io{
	class IOException
	class RandomAccessFile
}

package java.lang{
	class CharSequence
	class IllegalArgumentException
	class Integer
	class InterruptedException
	class RuntimeException
	class SecurityException
	class System
	class Thread
	class String
}

package java.util{
	interface List
	class HashSet
}


	HostClipboardMonitor -->  HostClipboardCallback 
   	HostClipboardMonitor -->  String 
   	HostClipboardMonitor -->  RandomAccessFile
 	HostClipboardMonitor -->  System
   	HostClipboardMonitor -->  IOException 
   	HostClipboardMonitor -->  Thread
   	HostClipboardMonitor -->  Integer
   	HostClipboardMonitor -->  InterruptedException
   	HostClipboardMonitor -->  Slog



	ClipboardService -->  String
    ClipboardService -->  SystemProperties
    ClipboardService -->  IActivityManager
    ClipboardService -->  IUserManager
    ClipboardService -->  PackageManager
    ClipboardService -->  AppOpsManager
    ClipboardService -->  IBinder
    ClipboardService -->  HostClipboardMonitor
    ClipboardService -->  Thread
    ClipboardService -->  SparseArray
    ClipboardService -->  PerUserClipboard
    ClipboardService -->  Context
    ClipboardService -->  ActivityManager
    ClipboardService -->  ServiceManager
    ClipboardService -->  RemoteException 
    ClipboardService -->  Slog
    ClipboardService -->  HostClipboardCallback
    ClipboardService -->  ClipData                             
    ClipboardService -->  ClipData.Item
    ClipboardService -->  ClipboardImpl                    
    ClipboardService -->  UserHandle
    ClipboardService -->  List
    ClipboardService -->  UserInfo
    ClipboardService -->  Binder
    ClipboardService -->  ClipDescription
    ClipboardService -->  System
    ClipboardService -->  ListenerInfo
    ClipboardService -->  Uri
    ClipboardService -->  ContentProvider
    ClipboardService -->  Intent
    ClipboardService -->  Process
    ClipboardService -->  IPackageManager
    ClipboardService -->  AppGlobals
    ClipboardService -->  PackageInfo
    ClipboardService -->  IllegalArgumentException
    ClipboardService -->  SecurityException



	ListenerInfo -->  String
 

	PerUserClipboard --> RemoteCallbackList
	PerUserClipboard --> IOnPrimaryClipChangedListener
	PerUserClipboard --> ClipData
	PerUserClipboard --> HashSet
	PerUserClipboard --> String


	ClipboardImpl -->  Parcel
	ClipboardImpl -->  RemoteException 
	ClipboardImpl -->  RuntimeException
	ClipboardImpl -->  SecurityException
	ClipboardImpl -->  Slog
	ClipboardImpl -->  ClipData
	ClipboardImpl -->  String
	ClipboardImpl -->  IllegalArgumentException
	ClipboardImpl -->  HostClipboardMonitor
	ClipboardImpl -->  Binder
	ClipboardImpl -->  AppOpsManager
	ClipboardImpl -->  UserHandle
	ClipboardImpl -->  PerUserClipboard
	ClipboardImpl -->  List
	ClipboardImpl -->   UserInfo    
	ClipboardImpl -->  IUserManager
	ClipboardImpl -->  UserManager
	ClipboardImpl -->  ClipData.Item
	ClipboardImpl -->  ClipDescription
	ClipboardImpl -->  IOnPrimaryClipChangedListener
	ClipboardImpl -->  ListenerInfo
	ClipboardImpl -->  CharSequence
@enduml	


