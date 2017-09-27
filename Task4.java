@startuml

skinparam titleFontSize 14
title
  Association Legend
  |= --> |= Association found within |
  |<#1015a5>   | HostClipboardMonitor |
  |<#2c9e0f>   | ClipboardService |
  |<#6d12a5>   | ListenerInfo |
  |<#f9d939>   | PerUserClipboard |
  |<#000000>   | ClipboardImpl        |
end title
             
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


	HostClipboardMonitor -[#blue]->  HostClipboardCallback 
   	HostClipboardMonitor -[#blue]->  String 
   	HostClipboardMonitor -[#blue]->  RandomAccessFile
 	HostClipboardMonitor -[#blue]->  System
   	HostClipboardMonitor -[#blue]->  IOException 
   	HostClipboardMonitor -[#blue]->  Thread
   	HostClipboardMonitor -[#blue]->  Integer
   	HostClipboardMonitor -[#blue]->  InterruptedException
   	HostClipboardMonitor -[#blue]->  Slog



	ClipboardService -[#green]->  String
    ClipboardService -[#green]->  SystemProperties
    ClipboardService -[#green]->  IActivityManager
    ClipboardService -[#green]->  IUserManager
    ClipboardService -[#green]->  PackageManager
    ClipboardService -[#green]->  AppOpsManager
    ClipboardService -[#green]->  IBinder
    ClipboardService -[#green]->  HostClipboardMonitor
    ClipboardService -[#green]->  Thread
    ClipboardService -[#green]->  SparseArray
    ClipboardService -[#green]->  PerUserClipboard
    ClipboardService -[#green]->  Context
    ClipboardService -[#green]->  ActivityManager
    ClipboardService -[#green]->  ServiceManager
    ClipboardService -[#green]->  RemoteException 
    ClipboardService -[#green]->  Slog
    ClipboardService -[#green]->  HostClipboardCallback
    ClipboardService -[#green]->  ClipData                             
    ClipboardService -[#green]->  ClipData.Item
    ClipboardService -[#green]->  ClipboardImpl                    
    ClipboardService -[#green]->  UserHandle
    ClipboardService -[#green]->  List
    ClipboardService -[#green]->  UserInfo
    ClipboardService -[#green]->  Binder
    ClipboardService -[#green]->  ClipDescription
    ClipboardService -[#green]->  System
    ClipboardService -[#green]->  ListenerInfo
    ClipboardService -[#green]->  Uri
    ClipboardService -[#green]->  ContentProvider
    ClipboardService -[#green]->  Intent
    ClipboardService -[#green]->  Process
    ClipboardService -[#green]->  IPackageManager
    ClipboardService -[#green]->  AppGlobals
    ClipboardService -[#green]->  PackageInfo
    ClipboardService -[#green]->  IllegalArgumentException
    ClipboardService -[#green]->  SecurityException



	ListenerInfo -[#purple]->  String
 

	PerUserClipboard -[#orange]-> RemoteCallbackList
	PerUserClipboard -[#orange]-> IOnPrimaryClipChangedListener
	PerUserClipboard -[#orange]-> ClipData
	PerUserClipboard -[#orange]-> HashSet
	PerUserClipboard -[#orange]-> String


	ClipboardImpl -[#black]->  Parcel
	ClipboardImpl -[#black]->  RemoteException 
	ClipboardImpl -[#black]->  RuntimeException
	ClipboardImpl -[#black]->  SecurityException
	ClipboardImpl -[#black]->  Slog
	ClipboardImpl -[#black]->  ClipData
	ClipboardImpl -[#black]->  String
	ClipboardImpl -[#black]->  IllegalArgumentException
	ClipboardImpl -[#black]->  HostClipboardMonitor
	ClipboardImpl -[#black]->  Binder
	ClipboardImpl -[#black]->  AppOpsManager
	ClipboardImpl -[#black]->  UserHandle
	ClipboardImpl -[#black]->  PerUserClipboard
	ClipboardImpl -[#black]->  List
	ClipboardImpl -[#black]->   UserInfo    
	ClipboardImpl -[#black]->  IUserManager
	ClipboardImpl -[#black]->  UserManager
	ClipboardImpl -[#black]->  ClipData.Item
	ClipboardImpl -[#black]->  ClipDescription
	ClipboardImpl -[#black]->  IOnPrimaryClipChangedListener
	ClipboardImpl -[#black]->  ListenerInfo
	ClipboardImpl -[#black]->  CharSequence
@enduml	


