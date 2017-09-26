@startuml
                
package android.os{
	class Binder
	interface IBinder
	interface IInterface
}

package com.android.server.clipboard{
	class ClipboardService 
	class HostClipboardMonitor
	class PerUserClipboard
	class ClipboardImpl                    
	class ListenerInfo
}

package java.lang{
	class Object
	interface Runnable
}

package android.content{
	class IClipboard.Stub
	interface IClipboard
}

package com.android.server{
	class SystemService
}

SystemService <|-- ClipboardService
Object <|-- SystemService

Object <|-- HostClipboardMonitor
Runnable <|.. HostClipboardMonitor 


Object <|-- PerUserClipboard

IClipboard.Stub <|-- ClipboardImpl
Binder <|-- IClipboard.Stub
IClipboard <|.. IClipboard.Stub
Object <|-- Binder
IBinder <|.. Binder
IInterface <|-- IClipboard

Object <|-- ListenerInfo

@enduml	


