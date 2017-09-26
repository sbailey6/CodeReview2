/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.clipboard;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.IClipboard;
import android.content.IOnPrimaryClipChangedListener;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.util.SparseArray;

import com.android.server.SystemService;

import java.util.HashSet;
import java.util.List;

import java.lang.Thread;
import java.lang.Runnable;
import java.lang.InterruptedException;
import java.io.IOException;
import java.io.RandomAccessFile;
//*************************************BLOCK-1 BEGINS	Author: Rashika Gupta***********************************************************
// The following class is Android Emulator specific. It is used to read and
// write contents of the host system's clipboard.

/* Class name: HostClipboardMonitor
             : implements  Runnable class
  Method name: 1. onHostClipboardUpdated
               2. openPipe
  
Object   : 1. mPipe of RandomAccessFile class
            --> private access modifier
            --> declared null 
           2. mHostClipboardCallback of HostClipboardCallback class
Variable:    PIPE_NAME
            --> string type
            --> private access modifier
            --> string name - pipe:clipboard
            --> static and final- we can't chnge the values.

*/ 

class HostClipboardMonitor implements Runnable {
    
    /* interface : HostClipboardCallback
     method :  onHostClipboardUpdated
              : void type
              : Parameter - content of string data type
    */        
    public interface HostClipboardCallback {
        void onHostClipboardUpdated(String contents);
    }

    // Objects and variables
    
    private RandomAccessFile mPipe = null;
    private HostClipboardCallback mHostClipboardCallback;
    private static final String PIPE_NAME = "pipe:clipboard";
    private static final String PIPE_DEVICE = "/dev/qemu_pipe";

    
    
    private void openPipe() {
        
        // try-catch block for exception 
        try {
            // String.getBytes doesn't include the null terminator,
            // but the QEMU pipe device requires the pipe service name
            // to be null-terminated.
            
            // array  b of array byte[] of length "PIPE_NAME.length() + 1"
            byte[] b = new byte[PIPE_NAME.length() + 1];
            // setting b array to zero 
            // Parameter of array : PIPE_NAME.length()
            b[PIPE_NAME.length()] = 0;
            // copying the values of the array.
            System.arraycopy(
                PIPE_NAME.getBytes(),
                0,
                b,
                0,
                PIPE_NAME.length());
            
            // Object creation
            // read and write
            mPipe = new RandomAccessFile(PIPE_DEVICE, "rw");
            // writing b array 
            mPipe.write(b);
            
            // Catch block for IO exception
        } catch (IOException e) {
            
            // try-catch block for IO Exception
            try {
                
                // if mPipe not equals to null then close mPipe 
                if (mPipe != null) mPipe.close();
            } catch (IOException ee) {}
            // setting object to null 
            mPipe = null;
        }
    }

    
    // constructor of the class 
    // Parameter : Object of HostClipboardCallback
    public HostClipboardMonitor(HostClipboardCallback cb) {
        // passing the value of object cb to mHostClipboardCallback
        mHostClipboardCallback = cb;
    }

    
    // Overiding the run method 
    @Override
    public void run() {
        // try catch block for catching IO Exception
        while(!Thread.interrupted()) {
            try {
                // There's no guarantee that QEMU pipes will be ready at the moment
                // this method is invoked. We simply try to get the pipe open and
                // retry on failure indefinitely.
                // while mPipe is null, calling the method openPipe and letting the thread sleep for 100 ms
                while (mPipe == null) {
                    openPipe();
                    Thread.sleep(100);
                }
                
                // declaring the variable size to the value returned from mPipe.readInt method 
                int size = mPipe.readInt();
                // reversing the value of size 
                size = Integer.reverseBytes(size);
                
                // declaring a new array receivedData of byte type and initializing the size of the array to the value of "size" variable obtained above 
                
                byte[] receivedData = new byte[size];
                
                // calling the method readFully of mPipe and passing the parameter receivedData which is an array 
                mPipe.readFully(receivedData);
                
                // calling the method onHostClipboardUpdated of mHostClipboardCallback and passing the parameter: creating new object of String array 
                mHostClipboardCallback.onHostClipboardUpdated(
                    new String(receivedData));
                    
                    // catch block for catching IO exception
            } catch (IOException e) {
                try {
                    
                    // closing mPipe 
                    mPipe.close();
                } catch (IOException ee) {}
                // setting mPipe to null 
                mPipe = null;
            } catch (InterruptedException e) {}
        }
    }

    
    // method setHostClipboard : void type
    //                          : Parameter- is a string "content"
    
    public void setHostClipboard(String content) {
        
        // try catch block for IO Exception
        try {
            // if mPipe is not null then calling the writeInt method of mPipe. Calculating the len of content and reversing its bytes and passing it as a parameter to the writeInt method 
            if (mPipe != null) {
                mPipe.writeInt(Integer.reverseBytes(content.getBytes().length));
                mPipe.write(content.getBytes());
            }
            
            // catching the excepton and dispaying the string "HostClipboardMonitor", "Failed to set host clipboard " and the value from e.getMessage 
        } catch(IOException e) {
            Slog.e("HostClipboardMonitor",
                   "Failed to set host clipboard " + e.getMessage());
        }
    }
}
//*********************************************** BLOCK-2 BEGINS    Author: Drishti Arora***********************************************************
/**
 * Implementation of the clipboard for copy and paste.
 */
 
 /*

Class : ClipboardService
      : Inherits SystemService class
 
 */
public class ClipboardService extends SystemService {


/* Variables: 1. TAG 
            --> String type 
            --> Static and final 
            2. IS_EMULATOR
            --> boolean type- returns true or false
*/
    private static final String TAG = "ClipboardService";
    private static final boolean IS_EMULATOR =
        SystemProperties.getBoolean("ro.kernel.qemu", false);

        /*
        Objects : mAm of IActivityManager
                : mUm of IUserManager
                : mPm of PackageManager
                :mAppOps of AppOpsManager
                : mPermissionOwner of IBinder
        
        */
        
    private final IActivityManager mAm;
    private final IUserManager mUm;
    private final PackageManager mPm;
    private final AppOpsManager mAppOps;
    private final IBinder mPermissionOwner;
    
    // mHostClipboardCallback of HostClipboardMonitor declared as null
    private HostClipboardMonitor mHostClipboardMonitor = null;
    
    // thread mHostClipboardMonitor declared null 
    private Thread mHostMonitorThread = null;

    
    // declaring a sparse array of PerUserClipboard type 
    private final SparseArray<PerUserClipboard> mClipboards = new SparseArray<>();

    /**
     * Instantiates the clipboard.
     */
     
     // Constructor 
     // passing the parameter object context of type Context 
    public ClipboardService(Context context) {
        // Parent class called 
        super(context);

        // setting the variables to the values obtained from the methods 
        mAm = ActivityManager.getService();
        mPm = getContext().getPackageManager();
        mUm = (IUserManager) ServiceManager.getService(Context.USER_SERVICE);
        mAppOps = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        
        // creating object of IBinder and setting it to null 
        IBinder permOwner = null;
        
        //try catch block for catching the exception 
        try {
            
            // passing the value to the object 
            permOwner = mAm.newUriPermissionOwner("clipboard");
        } catch (RemoteException e) {
            Slog.w("clipboard", "AM dead", e);
        }
        // passing the value of permOwner to mPermissionOwner
        mPermissionOwner = permOwner;
        
        // if IS_EMULATOR is false
        if (IS_EMULATOR) {
            
            // creating an object 
            mHostClipboardMonitor = new HostClipboardMonitor(
                new HostClipboardMonitor.HostClipboardCallback() {
                    
                    
                    // Overiding the onHostClipboardUpdated method
                    // Parameter: contents- string type 
                    @Override
                    public void onHostClipboardUpdated(String contents){
                        
                        // creating and initializing the object "clip" of ClipData 
                        // new array of String type 
                        ClipData clip =
                            new ClipData("host clipboard",
                                         new String[]{"text/plain"},
                                         new ClipData.Item(contents));
                        synchronized(mClipboards) {
                            setPrimaryClipInternal(getClipboard(0), clip);
                        }
                    }
                });
                
                // setting the thread object and passing the object mHostClipboardMonitor 
            mHostMonitorThread = new Thread(mHostClipboardMonitor);
            
            // starting the thread
            mHostMonitorThread.start();
        }
    }
    
    // Overiding method onStart

    @Override
    public void onStart() {
        publishBinderService(Context.CLIPBOARD_SERVICE, new ClipboardImpl());
    }

    // overriding onCleanupUser
    // passed parameter"userId" of int type
    @Override
    public void onCleanupUser(int userId) {
        synchronized (mClipboards) {
            
            // removing the userId 
            mClipboards.remove(userId);
        }
    }

    
    /* Class : ListenerInfo
    Variable: 1. mUid
               --> int type 
               --> final- value cant change
            2. mPackageName
            --> String type 
            --> final 
               
               
    */
    private class ListenerInfo {
        final int mUid;
        final String mPackageName;
        
        // calling ListenerInfo.
        // Parameters: 1. uid- int type
                    // 2. packageName- String type
                       
                       
        ListenerInfo(int uid, String packageName) {
            // seting vlue of uid to mUid and packageName to mPackageName
            mUid = uid;
            mPackageName = packageName;
        }
    }

    
    // Class : PerUserClipboard
    // Variable : userid- int type and final 
    private class PerUserClipboard {
        final int userId;

        // object creation 
        final RemoteCallbackList<IOnPrimaryClipChangedListener> primaryClipListeners
                = new RemoteCallbackList<IOnPrimaryClipChangedListener>();

        ClipData primaryClip;
// HashSet 
        final HashSet<String> activePermissionOwners
                = new HashSet<String>();

        PerUserClipboard(int userId) {
            this.userId = userId;
        }
    }
//***********************************************BLOCK-3: BEGINS    Author: Sujit Kumar***********************************************************
   /* 
1. Class name: ClipboardImpl
             A child class of ClipboardImpl is derived from  IClipboard.Stub with private scope
             visibiility: private
                class is private. hus, data and methods are scoped only
             
        1.1 Method: onTransact(), overridden by the definition in this class, i.e. child 
            This method calls an IBinder object and receives a call from Binder object. The call is synchronous, and thus, it calls IBinder object 
            only when it has returned from Binder object.        
            
            It takes four arguments: 
                1.1.1 code, flags of type int
                1.1.2 data, reply of type Parcel
            Return Type: returns true or false upon the success/failure of the transaction
        
        
        1.2 Method: setPrimaryClip(), ovverridden by the definition in this class, i.e. child 
                This method checks for user privilidges,fethes user profiles, ensures whether lock can be granted for the thread using monitors. If
                the priviledges are valid for the user, it instanriates an object of ClipData for the user.
            visibility: public
            
            Retrun type: void
            
            It takes two arguments: 
                1.2.1.1 an object of Clipdata 
                1.2.1.2 the name of the calling package
            
            variables: 
                1.2.2.1 Item- an object of class ClipData
                1.2.2.2 canCopy- local variable type boolean variable
                1.2.2.3 related - local variable of type List
                
**method: synchronized() ensures that only one thread is executing at one time inside the process associated with this object               
*/        

 
     
    
    private class ClipboardImpl extends IClipboard.Stub {
        @Override
        // The method returns true or false as per the transaction success/failure
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            try {
            //The onTransact() method is invoked from the parent class definition
             
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf("clipboard", "Exception: ", e);
                }
                throw e;
            }

        }
        
        //clip is an object of class Clipdata, and callingPackage is the package that has invoked the transaction
        //returns void
        @Override
        public void setPrimaryClip(ClipData clip, String callingPackage) {
        //method: synchronized() ensures that only one thread is executing at one time inside the process associated with this object. 
        //The object aquires a lock using monitors and all other threads seeking the same resources are put in suspended state
            synchronized (this) {
                //checks if there is new text in clip
                //method getItemCount() is defined in class Clipdata, and returns size of the string  
                if (clip != null && clip.getItemCount() <= 0) {
                    throw new IllegalArgumentException("No items");
                }
                //getItemAt() is defined in class ClipData, and returns the text in the indexed position
                //mHostClipboardMonitor is an object of HostClipboardMonitor class which, in turn, implements runnable
                if (clip.getItemAt(0).getText() != null &&
                    mHostClipboardMonitor != null) {
                    mHostClipboardMonitor.setHostClipboard(
                        clip.getItemAt(0).getText().toString());
                }
                //method: callingUid is defined in class Binder which implements IBinder interface
                //method: callingUid rerurns the userHandle of the process that has invoked the transaction
                final int callingUid = Binder.getCallingUid();
                //method: clipboardAccessAllowed checks if the calling package is allowed to access clipboard
                if (!clipboardAccessAllowed(AppOpsManager.OP_WRITE_CLIPBOARD, callingPackage,
                            callingUid)) {
                    return;
                }
                //method: checkDataOwnerLocked invokes checkItemOwnerLocked and checks the lock taken by each user id on each data item
                checkDataOwnerLocked(clip, callingUid);
                //the variable userId has been declared as final, meaning the assigned value will not change throughout the program
                final int userId = UserHandle.getUserId(callingUid);
                PerUserClipboard clipboard = getClipboard(userId);
                //primaryClip is assigned the value null to revoke clipboard access of the user
                revokeUris(clipboard);
                //reset the clipboard access, reset clipboard to null, brodcast the change to all the users, and restores the user permissions
                setPrimaryClipInternal(clipboard, clip);
                //return the profiles of the user id passed as argument
                //if the returned profile is not null, invoke user manager to fetch user restrictions, reset the user permissions and initialize clipboard with null  
                List<UserInfo> related = getRelatedProfiles(userId);
                if (related != null) {
                    int size = related.size();
                    if (size > 1) { // Related profiles list include the current profile.
                        boolean canCopy = false;
                        try {
                            canCopy = !mUm.getUserRestrictions(userId).getBoolean(
                                    UserManager.DISALLOW_CROSS_PROFILE_COPY_PASTE);
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
                        }
                        // Copy clip data to related users if allowed. If disallowed, then remove
                        // primary clip in related users to prevent pasting stale content.
                        if (!canCopy) {
                            clip = null;
                        } else {
                            // We want to fix the uris of the related user's clip without changing the
                            // uris of the current user's clip.
                            // So, copy the ClipData, and then copy all the items, so that nothing
                            // is shared in memmory.
                            clip = new ClipData(clip);
                            for (int i = clip.getItemCount() - 1; i >= 0; i--) {
                                clip.setItemAt(i, new ClipData.Item(clip.getItemAt(i)));
                            }
                            clip.fixUrisLight(userId);
                        }
                        for (int i = 0; i < size; i++) {
                            int id = related.get(i).id;
                            if (id != userId) {
                                setPrimaryClipInternal(getClipboard(id), clip);
                            }
                        }
                    }
                }
            }
        }

//***********************************************BLOCK-4: BEGINS      Author: Shawn Bailey***********************************************************
         /*Getter method which returns the primaryClip found inside the private inner PerUserClipboard,
		 which is defined at the end of BLOCK-2 in this file.

		 The parameter, String pkg, corresponds to the package who is trying to access and read the primaryClip.
         It is used to check for permissions. Returns null if the pkg does not have permission to read the clip,
         otherwise returns the primaryClip.
         */

        /* Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        /*ClipData is imported from android.content.ClipData. 
         ClipData contains meta-data about data that has been copied by the user.
         This could be plain text, an image, a URL, etc. 
         The ClipData is meant to allow pasting or dropping the data into an application,
         such that the application can correctly interpret and represent that data.
        */
        public ClipData getPrimaryClip(String pkg) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            /*Before the primary Clip is returned, the method first checks if the user trying to access it 
         	has a user id which has access to read the clip. If it does not, the method returns null instead as a safeguard,
         	indicating the user does not have the rights to access the primaryClip for reading purposes.
        	In order to understand this check, first we need to understand some classes. 

         	The Binder class is imported from android.os.Binder, and is a inter-process-communication primitive. 
        	It helps delegate communication among multiple processes. The method getCallingUid() returns the linux 
         	user id of the process which sent the current work to be done, in other words, the uid of the caller.
	
         	The AppOpsManager class is imported from android.app.AppOsManager. This class provides an API for
        	keeping track of application operations. AppOpsManager.OP_READ_CLIPBOARD is just a constant int equal to
         	29, which corresponds to the reading operation for a clipboard. 
	
        	The Binder's uid and the read operation and the calling package are all used as parameters to the method
        	cliboardAccessAllowed(), which returns true or false, depending on if the calling package can
        	access the clip for reading at the moment getPrimaryClip is called. (The details of clipboardAccessAllowed
        	can be found in BLOCK-10 of this file.)
        	*/
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, pkg,
                            Binder.getCallingUid())) {
                    return null;
                }
            /* If the calling package does have permission to read the primaryClip, we add that calling package
            to the active owners that have a lock. The details of this method call can be found in 
            BLOCK-9 of this file.
            */
                addActiveOwnerLocked(Binder.getCallingUid(), pkg);
	        /*This method calls the getter method getClipboard() which returns a PerUserClipboard object.
	 		The getClipboard() method is defined in BLOCK-5 of this file.
	 		The PerUserClipboard class is a private inner class defined in this file, at the end of BLOCK-2. 
	 		The PerUserClipboard class has the primaryClip, a RemoteCallbackList of those that listen to the clip, and a hash set of active owners and their permissions
	 		associated with the primaryClip. The primaryClip also has a userId associated with it, which is
			initialized by the PerUserClipboard constructor.  
	 		From that PerUserClipboard object, we extract it's ClipData primaryClip and return that object reference.*/
                return getClipboard().primaryClip;
            }
        }

        /*Getter method which returns a ClipDescription which describes what type of 
        data is contained inside the ClipData primaryClip of the private inner class PerUserClipboard, found in 
        BLOCK-2 of this file. 

        The parameter, String callingPackage, corresponds to the package who is trying to access and read the primaryClip.
        It is used to check for permissions.

        Returns null if the calling package does not have access to read the clip or the clip is itself null,
        otherwise returns the ClipDescription associated with primaryClip.*/

        /* Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        public ClipDescription getPrimaryClipDescription(String callingPackage) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            /*checks if the user has a user id which has access to read the clip. If it does not, the method 
            returns null instead as a safeguard, indicating the user does not have the rights to access the 
            primaryClip for reading purposes. 
        	*/
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return null;
                }
            /*This method calls the getter method getClipboard() which returns a PerUserClipboard object.
	 		The getClipboard() method is defined in BLOCK-5 of this file.
	 		The PerUserClipboard class is a private inner class defined in this file, at the end of BLOCK-2.
	 		The returned object reference is saved in clipboard*/
                PerUserClipboard clipboard = getClipboard();
            /*If the primaryClip is null, return null for the description.
            Otherwise, we return the description of the primaryClip stored inside PerUserClipboard clipboard
            by using the getter method getDescription(), defined in ClipData.java.
            */
                return clipboard.primaryClip != null ? clipboard.primaryClip.getDescription() : null;
            }
        }

        /*The parameter, String callingPackage, corresponds to the package who is trying to access and read the primaryClip.
        It is used to check for permissions.

        Returns whether the callingPackage has permission to read ClipData primaryClip stored inside
        the private inner class PerUserClipboard, found in BLOCK-2. 
        Returns false if callPackage doesn't have access or primaryClip is null, 
        otherwise returns true. */

        /* Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        public boolean hasPrimaryClip(String callingPackage) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            /*checks if the user has a user id which has access to read the clip. If it does not, the method 
            returns false, indicating the user does not have the rights to access the 
            primaryClip for reading purposes. 
        	*/
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return false;
                }
            /*if callingPackage does have access to the primaryClip, then as long as the clip is not null, 
            we return true. if the primaryClip is null, the method returns false. */
                return getClipboard().primaryClip != null;
            }
        }
        /*The first parameter, IOnPrimaryClipChangedistener listener, comes from 
        the import of android.content.IOnPrimaryClipChangedListener.  This is an aidl file,
        which stands for android interface definition language. An aidl file is used to define
        the programming interface that a client and service agree upon in order to communicate 
        using IPC. The import is a oneway interface. Here, the oneway keyword modifies the behavior of 
        remote calls, so that when used, the call does not block. The call will instead send the 
        transaction data and immediately return.

        The oneway interface has one method to implement, called void dispatchPrimaryClipChanged(). This method gets
        its definition in java/android/content/ClipBoardManager.java
        
        This interface is also part of IClipboard.aidl, which is what this private inner class implements

        The second parameter, String callingPackage, corresponds to the package who is trying to listen to the primaryClip.
        
        This method registers listener to the RemoteCallbackList associated with the DataClip primaryClip stored in 
        PerUserClipboard, a private inner class defined in BLOCK-2 of this file. It associates the callingPackage with the 
        listener being registered*/

        /* Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener,
                String callingPackage) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            	/*register callingPackage as a listener for the ClipData primaryClip stored inside the
            	private inner class PerUserClipboard, found in BLOCK-2

                Here, we grab the RemoteCallbackList of IOnPrimaryClipChangedListener stored in PerUserClipboard
                RemoteCallback comes from the import android.os.RemoteCallbackList.

                Performs locking of the underlying list of interfaces to deal with
                multithreaded incoming calls, and a thread-safe way to iterate over a
                snapshot of the list without holding its lock. It also cleans the list if a process goes away,
                using an IBinder.DeathRecipient to keep track of such occurences. 

                Below, register takes two arguments, the second being optional. The first is a callback interface,
                in this case an IOnPrimaryClipChangedListener, and the second is an Object cookie, which provides additional
                data to be associated with this call back. In this case, cookie is of type ListenerInfo, 
                which stores the caller's user id and the callingPackage associated with it. Note that everything 
                in java extends the Object class, giving quite a bit of flexibility for what is a legal cookie here.

                ListenerInfo is a private inner class of this file, defined in BLOCK-2.
                */
                getClipboard().primaryClipListeners.register(listener,
                        new ListenerInfo(Binder.getCallingUid(), callingPackage));
            }
        }

        /*This method unregisters listener, an IOnPrimaryClipChangedListener interface, from the 
        RemoteCallbackList associated with ClipData primaryClip stored in PerUserClipboard,
        a private inner class defined in BLOCK-2 of this file. */

         /*Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            	/*unregister listener from the ClipData primaryClip stored inside the
            	private inner class PerUserClipboard, found in BLOCK-2*/
                getClipboard().primaryClipListeners.unregister(listener);
            }
        }

        /*The parameter, String callingPackage, corresponds to the package which is being checked for
        ClipBoard Text

        Returns false if callingPackage doesn't have permision to access the clip, or there is something
        wrong with extracting meaningful text. Otherwise, returns true.*/

         /* Since this class ClipBoardImpl extends IClipboard.Stub, this method overrides  
		 that class's method. */
        @Override
        public boolean hasClipboardText(String callingPackage) {
        	/*synchronized is a keyword meant to enforce concurrency without race conditions and deadlocks. 
        	It is built around an intrinsic lock. Here, this statement measn that this class will provide the intrinsic
        	lock used to enforce concurrent behavior in a safe manner. 

        	 This ensures only one thread can be executing this code at a time, and all other 
        	 calling threads will be forced to block and wait for their exclusive turn to use this operation*/
            synchronized (this) {
            /*checks if the user has a user id which has access to read the clip. If it does not, the method 
            returns false, indicating the user does not have the rights to access the 
            primaryClip for reading purposes, and thus doesn't have any Clipboard Text available. 
            In order to understand this check, first we need to understand some classes. 

         	The Binder class is imported from android.os.Binder, and is a inter-process-communication primitive. 
        	It helps delegate communication among multiple processes. The method getCallingUid() returns the linux 
         	user id of the process which sent the current work to be done, in other words, the uid of the caller.
	
         	The AppOpsManager class is imported from android.app.AppOsManager. This class provides an API for
        	keeping track of application operations. AppOpsManager.OP_READ_CLIPBOARD is just a constant int equal to
         	29, which corresponds to the reading operation for a clipboard. 
	
        	The Binder's uid and the read operation and the calling package are all used as parameters to the method
        	cliboardAccessAllowed(), which returns true or false, depending on if the calling package can
        	access the clip for reading at the moment getPrimaryClip is called. (The details of clipboardAccessAllowed
        	can be found in BLOCK-10 of this file.)
        	*/
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return false;
                }
            /*This method calls the getter method getClipboard() which returns a PerUserClipboard object.
	 		The getClipboard() method is defined in BLOCK-5 of this file.
	 		The PerUserClipboard class is a private inner class defined in this file, at the end of BLOCK-2.
	 		The returned object reference is saved in clipboard*/
                PerUserClipboard clipboard = getClipboard();
            /*If clipboard is not null and ClipData primaryClip's text inside clipboard is not null
            nor an empty CharSequence,  return true */
                if (clipboard.primaryClip != null) {
                    CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
                    return text != null && text.length() > 0;
                }
            /*otherwise return false*/
                return false;
            }
        }
    };
//*************************************BLOCK-5 BEGINS	Author: Rashika Gupta***********************************************************
/* Method name : getClipboard
                --> Type : PerUserClipboard
                --> returns Calling user id from UserHandle

                */          
 private PerUserClipboard getClipboard() {
        return getClipboard(UserHandle.getCallingUserId());
    }

    
// userId parameter of int data type 
    private PerUserClipboard getClipboard(int userId) {
        
        // synchronized keyword for thread
        // Parameters : mClipboards
        synchronized (mClipboards) {
            
            // getting the user id from mClipboards and setting to the object "puc" of PerUserClipboard
            PerUserClipboard puc = mClipboards.get(userId);
            
            // if puc is null then pass the user id from PerUserClipboard to the object "puc"
            if (puc == null) {
                puc = new PerUserClipboard(userId);
                // putting the values user id , puc 
                mClipboards.put(userId, puc);
            }
            // returning puc 
            return puc;
        }
    }
//*********************************************** BLOCK-6: BEGINS     Author: Drishti Arora***********************************************************
   List<UserInfo> getRelatedProfiles(int userId) {
        final List<UserInfo> related;
        final long origId = Binder.clearCallingIdentity();
       
    // Try catch block for Remote Exception
       try {
            related = mUm.getProfiles(userId, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
            return null;
        } finally{
            Binder.restoreCallingIdentity(origId);
        }
        return related;
    }
//*************************************BLOCK-7 BEGINS	Author: Rashika Gupta***********************************************************
/* Method :   setPrimaryClipInternal
            --> Parameter: clipboard of PerUserClipboard type and clip of ClipData type 
*/
  void setPrimaryClipInternal(PerUserClipboard clipboard, ClipData clip) {
      // clearing the active Permission Owners of clipboard
        clipboard.activePermissionOwners.clear();
        
        // if clip and primaryClip of clip is null then return nothing
        if (clip == null && clipboard.primaryClip == null) {
            return;
        }
        
        // passing the value of clip to primaryClip of clipboard
        clipboard.primaryClip = clip;
        if (clip != null) {
            // creating object of ClipDescription and setting the value of the function getDescription of clip to it.
            final ClipDescription description = clip.getDescription();
            if (description != null) {
                // if description is not null, setting the time 
                description.setTimestamp(System.currentTimeMillis());
            }
        }
        /* variable ident of long type
         clear calling identity method is called of binder and its value is set to ident
        it clears the identity of the caller
    */
        final long ident = Binder.clearCallingIdentity();
        final int n = clipboard.primaryClipListeners.beginBroadcast();
        try {
            // from i=0 to n and incrementing it until i< n 
            for (int i = 0; i < n; i++) {
                // try catch block fot catching Remote Exception 
                try {
                    // Object li of ListenerInfo
                    // getting the broadcast cookie of ith value of primaryClipListeners of clipboard and typecasting it to ListenerInfo
                    ListenerInfo li = (ListenerInfo)
                            clipboard.primaryClipListeners.getBroadcastCookie(i);

                    if (clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, li.mPackageName,
                                li.mUid)) {
                        // get ith broadcast item and changed dispatched Primary clip
                        clipboard.primaryClipListeners.getBroadcastItem(i)
                                .dispatchPrimaryClipChanged();
                    }
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
        } finally {
            // Broadcast finished
            clipboard.primaryClipListeners.finishBroadcast();
            // Restoring the calling identity of the passed ident 
            Binder.restoreCallingIdentity(ident);
        }
    }
//*********************************************** BLOCK-8: BEGINS     Author: Drishti Arora***********************************************************
/* Method name: checkUriOwnerLocked

Parameter: 1. uri object of type Uri
           2. uid of int type 

*/

  private final void checkUriOwnerLocked(Uri uri, int uid) {
      
      // if content is not equal to the getScheme value of uri then return nothing
        if (!"content".equals(uri.getScheme())) {
            return;
        }
        
        // ident of long type which takes the value of the cleared calling identity of Binder
        long ident = Binder.clearCallingIdentity();
        try {
            // This will throw SecurityException for us.
 
/* checkGrantUriPermission method is called of mAm which checks the granted uri permission which takes the parameters uid, null, 
uri value of getUriWithoutUserId method of contentProvider, FLAG_GRANT_READ_URI_PERMISSION of Intent which reads the granted uri permission 
and ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(uid)
 */ 
 
 mAm.checkGrantUriPermission(uid, null, ContentProvider.getUriWithoutUserId(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(uid)));
                    // catchind REMOTE exception
        } catch (RemoteException e) {
        } finally {
            // it restores the calling identity of the passed ident 
            // Method name: restoreCallingIdentity of binder
            Binder.restoreCallingIdentity(ident);
        }
    }

    
    /* Method name : checkDataOwnerLocked
                   : void type
                Parameter: 1. item of ClipData
                           2. uid of type int 
        --> checking if the item owner is locked 
    */
    private final void checkItemOwnerLocked(ClipData.Item item, int uid) {
        // if null 
        if (item.getUri() != null) {
            // check if locked 
            checkUriOwnerLocked(item.getUri(), uid);
        }
        // getting intent of item and storing in intent variable of Intent 
        Intent intent = item.getIntent();
        
        // if intent not null and getData does not returns null 
        if (intent != null && intent.getData() != null) {
            // check if locked - uid 
            checkUriOwnerLocked(intent.getData(), uid);
        }
    }
    
    /* Method name: checkDataOwnerLocked
                  : void type and final
        Parameter: 1. data of ClipData type
                2. uid of int type
        Variable name: 1. N
                    --> int type
                    --> final
                    --> stores the item count of data

                */

    private final void checkDataOwnerLocked(ClipData data, int uid) {
        final int N = data.getItemCount();
        
        // from i=0 till i<N incrementing value of i and checking if item owners locked for passed uid, item at ith value of data
        for (int i=0; i<N; i++) {
            checkItemOwnerLocked(data.getItemAt(i), uid);
        }
    }
//*********************************************** BLOCK-9: BEGINS      Author: Sujit Kumar***********************************************************
/*
1. Method: grantUriLocked()
                This method sources user id, resets the identity of the current inter process communication's thread, grants permissions to the user, 
                and restores the identity of the current inter process communication's thread.
                
                The method is marked as final, indicating that it can not be changed throughout the program.
            visibility: private, accessible by the objects only of the class it is declared in
            
            Retrun type: void
                        
            It takes three arguments: 
                1.1.1 an object of abstract class Uri 
                1.1.2 calling package name, 
                1.1.3 user id
            
            variables: 
                1.2.1 Item- an object of class ClipData
                1.2.2 canCopy- local variable type boolean variable
                1.2.3 related - local variable of type List 
                
 2. Method: grantItemLocked() 
                grants lock on the resources to the thread using monitors.
                invokes method grantUriLocked with Clipdata object, Intent object  
                if the Clipdata/intent object is not null, lock is granted
            
            Return: void
            
            visibility: private, accessible by the objects only of the class it is declared in
            
            Takes three arguments: 
                2.1.1 Clipdata/Intent object 
                2.1.2 calling package
                2.1.3 user id

3. method: addActiveOwnerLocked()
            Resets the identity of the current inter process communication's thread, grants permissions to the user, 
            restores the identity of the current inter process communication's thread, and adds the user id in the set of owners of the current thread.
            
            Return: void         
            Takes two argumens: 
                3.1.1 user id 
                3.1.2 name of calling package 
            variables: 
                All these three variables are declared locally, and as final. This means they are locally scoped, and rvalue can not be 
                    re-assigned in the scope.
                3.2.1 pm = object of type IPackageManager returned by method getPackageManager()
                3.2.2 targetUserHandle = user Id (type integer) returned by method getCallingUserId()
                3.2.3 oldIdentity = identity of current thread (type long integer) returned by method clearCallingIdentity()

method: synchronized() ensures that only one thread is executing at one time inside the process associated with this object                
 */
                

//method: grantUriLocked
// invokes methods clearCallingIdentity and restoresCallingIdentity defined in public class Binder
    private final void grantUriLocked(Uri uri, String pkg, int userId) {
        //reset the identity of the current inter process communication's thread
        long ident = Binder.clearCallingIdentity();
        try {
            //get user id of the user from Uri details
            int sourceUserId = ContentProvider.getUserIdFromUri(uri, userId);
            uri = ContentProvider.getUriWithoutUserId(uri);
            mAm.grantUriPermissionFromOwner(mPermissionOwner, Process.myUid(), pkg,
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, sourceUserId, userId);
        } catch (RemoteException e) {
        } finally {
        ////restore the identity of the current inter process communication's thread
            Binder.restoreCallingIdentity(ident);
        }
    }
    
    //invokes method grantUriLocked with Clipdata object, Intent object  
    //if the Clipdata/intent object is not null, lock is granted
    
    private final void grantItemLocked(ClipData.Item item, String pkg, int userId) {
        if (item.getUri() != null) {
            grantUriLocked(item.getUri(), pkg, userId);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriLocked(intent.getData(), pkg, userId);
        }
    }
    //method: addActiveOwnerLocked receives two argumens: user id and calling package
    //
    private final void addActiveOwnerLocked(int uid, String pkg) {
    //name of package manager, targetUserHandle, old Identity is assigned as final, that means it will not change throughout the program
        final IPackageManager pm = AppGlobals.getPackageManager();
        final int targetUserHandle = UserHandle.getCallingUserId();
        final long oldIdentity = Binder.clearCallingIdentity();
        try {
        //information of the package is returned to pi
            PackageInfo pi = pm.getPackageInfo(pkg, 0, targetUserHandle);
            if (pi == null) {
                throw new IllegalArgumentException("Unknown package " + pkg);
            }
            if (!UserHandle.isSameApp(pi.applicationInfo.uid, uid)) {
                throw new SecurityException("Calling uid " + uid
                        + " does not own package " + pkg);
            }
        } catch (RemoteException e) {
            // Can't happen; the package manager is in the same process
        } finally {
            Binder.restoreCallingIdentity(oldIdentity);
        }
        // method: getClipboard()instantiates an object of class PerUserClipboard for each user id 
        PerUserClipboard clipboard = getClipboard();
        //Item count is calculated, item is locked, and permissions are added
        if (clipboard.primaryClip != null && !clipboard.activePermissionOwners.contains(pkg)) {
            final int N = clipboard.primaryClip.getItemCount();
            for (int i=0; i<N; i++) {
                grantItemLocked(clipboard.primaryClip.getItemAt(i), pkg, UserHandle.getUserId(uid));
            }
            clipboard.activePermissionOwners.add(pkg);
        }
    }



//*********************************************** BLOCK-10: BEGINS      Author: Shawn Bailey***********************************************************
    /*This is a private method. The final keywords means the method cannot be overriden by subclasses.
    It takes a Uri uri as a parameter, which comes from the import android.net.Uri. URI stands for 
    uniform resource identifiers. They are a compact string of characters for identifying an 
    abstract or physical resource. A website URL is an example, but URI's represent and identify
    many other resources.

    The "four main components" of a hierarchical URI consist of
    <scheme>://<authority><path>?<query>

    This method unlocks a Uri for reading and writing*/
    private final void revokeUriLocked(Uri uri) {
        /*Extract the user id from the provided Uri.

        ContentProviders are one of the basic components of Android applications, used to 
        supply applications with some sort of content. 
        Here, the second parameter seen in the getUserIdFromUri() method
        is the defaultUserId returned if the provided uri is null.
        The method is static, which is why it can be called on the class itself here.*/
        int userId = ContentProvider.getUserIdFromUri(uri,
                UserHandle.getUserId(Binder.getCallingUid()));
        /*Reset the identity of the incoming IPC on the current thread.  
        Returns an opaque token that can be used to restore the original calling 
        identity by passing it to Binder.restoreCallingIdentity(), which is done
        at the end of this method. The opaque token is being stored in long ident,
        which is short for identity.

        Again, clearCallingIdentity() is a static method in the Binder class,
        which is why it is being called on the Binder class itself.*/
        long ident = Binder.clearCallingIdentity();
        /* Next, we try to give the uri read and write permissions, thus effectively 
        revoking them from being locked out of this Clipboard service.*/
        try {
            /*set uri to the same uri, but without its user id*/
            uri = ContentProvider.getUriWithoutUserId(uri);
            /*mAm is a final instance of the interface IActivityManager, declared in this file in BLOCK-2.
            This method, revokeUriPermissionFromOwner(), can throw 
            a RemoteException, which is why we catch it if that occurs. 
			
			mPermissionOwner is an IBinder, but is being used as a IApplicationThread here. This can be done
			because IApplicationThread implements the interface IInterface, which is the base class for 
			Binder interfaces. IInterface has a getter method for IBinder called asBinder(). 
			When a new interface is being defined, such as IApplicationThread, it must derive from IInterface. 
			asBinder() retrieves the Binder object associated with IInterface. 

            An Intent is a description of an operation to be performed. Intent also has some modes predefined inside the class.
            Here, we are granting mPermissionOwner permission to perform read and write operations on the URI in the Intent's data and URIs
            specified in its ClipData. */
            mAm.revokeUriPermissionFromOwner(mPermissionOwner, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    userId);
        }
        /* we catch the RemoteException if it is thrown. Note that RemoteException
        is the parent exception for all Binder remote-invocation errors.*/  
        catch (RemoteException e) {
        }
        /*the finally block gets executed whether the try block executed without failure or whether
        something went wrong and a RemoteException was caught

        in either case, we restore the calling thread's identity using the opaque token
        stored in long ident.

        restoreCallingIdentity() is a static method in the Binder class,
        which is why it is being called on the Binder class itself.*/
        finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /*The parameter, ClipData.Item is coming from the public class Item found inside ClipData. 
    It represents a description of a single item in a ClipData.

    This method unlocks the item for reading and writing*/

    /*This is a private method. The final keywords means the method can not be overriden by subclasses.*/
    private final void revokeItemLocked(ClipData.Item item) {
        /*first, if the item's URI is not null, then we unlock the URI 
        by calling revokeUriLocked, which is the method commented above this one.*/
        if (item.getUri() != null) {
            revokeUriLocked(item.getUri());
        }
        /*next, we extract the Intent from within item.
        Recall  An Intent is a description of an operation to be performed. 
        Intent also has some modes predefined inside the class.*/
        Intent intent = item.getIntent();
        /*getData() is a method which  retrieves the data this intent is operating on.
        The data is represented as another URI.

        if the intent is not null and the data of the intent is also not null,
        then we unlock the URI of data for reading and writing using the revokeUriLocked,
        which is the method commented above this one.*/
        if (intent != null && intent.getData() != null) {
            revokeUriLocked(intent.getData());
        }
    }

    /*The parameter, PerUserClipboard clipboard is coming from the private inner class of this file,
    defined in BLOCK-2. It represents a description of a single item in a ClipData.

    This method unlocks all items for reading and writing within the clipboard*/

    /*This is a private method. The final keywords means the method can not be overriden by subclasses.*/
    private final void revokeUris(PerUserClipboard clipboard) {
    	/*if primaryClip is null inside clipboard, there is nothing to unlock so we return immediately*/
        if (clipboard.primaryClip == null) {
            return;
        }
        /*extract the number of items, from the ArrayList of Items called mItems, found in primaryClip, 
        which is again a ClipData instance. store the size in integer N, to be used in the coming for loop*/
        final int N = clipboard.primaryClip.getItemCount();
        /*iterate over each item in primaryClip, grabbing each item using the getItemAt() method,
        and unlock the items for reading and writing using the revokItemLocked method, commented
        above this one*/
        for (int i=0; i<N; i++) {
            revokeItemLocked(clipboard.primaryClip.getItemAt(i));
        }
    }

    /*returns true or false, indicating whether the clipboard can be accessed for a specified operation 
    by the application associated with the callingPackage and calling user id.*/
    private boolean clipboardAccessAllowed(int op, String callingPackage, int callingUid) {
        // Check the AppOp.
        /*mAppOps is an instance of AppOpsManager which is an API for tracking application
        operations.

        checkOp determines whether an application is able to perform the operation op.
        it returns an integer, which is being compared to AppOpsManager.MODE_ALLOWED.
        if the returned value does not indicate that the mode is allowed, then we immediately
        return false, inidicating the clipboard can not be accessed by the calling application*/
        if (mAppOps.checkOp(op, callingUid, callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }
        try {
            // Installed apps can access the clipboard at any time.
            /*Here, AppGlobals is a class which keeps track of certain globals related to a process.
			
			We grab the package manager and check if the application associated with callingPackage
			and callingUid is not an instant application. If it is not, that indicates we are dealing
			with an installed application, which can access the clipboard. If it is an instant application,
			it may not be able to access the clipboard, particularly if it is a background process.
            */
            if (!AppGlobals.getPackageManager().isInstantApp(callingPackage,
                        UserHandle.getUserId(callingUid))) {
                return true;
            }
            // Instant apps can only access the clipboard if they are in the foreground.
            /*return wherether the calling user id is in the foreground or not. if it is, is can access 
            the clipboard. if it is in the background, then it is not allowed to access the clipboard.*/
            return mAm.isAppForeground(callingUid);
        }/*if a RemoteException occured, the function could not determine if the application
        can access the clipboard, so it makes a failure log using Slog.java and returns false. */ 
        catch (RemoteException e) {
            Slog.e("clipboard", "Failed to get Instant App status for package " + callingPackage,
                    e);
            return false;
        }
    }
}
