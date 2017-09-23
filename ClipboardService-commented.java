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
    private class ClipboardImpl extends IClipboard.Stub {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf("clipboard", "Exception: ", e);
                }
                throw e;
            }

        }

        @Override
        public void setPrimaryClip(ClipData clip, String callingPackage) {
            synchronized (this) {
                if (clip != null && clip.getItemCount() <= 0) {
                    throw new IllegalArgumentException("No items");
                }
                if (clip.getItemAt(0).getText() != null &&
                    mHostClipboardMonitor != null) {
                    mHostClipboardMonitor.setHostClipboard(
                        clip.getItemAt(0).getText().toString());
                }
                final int callingUid = Binder.getCallingUid();
                if (!clipboardAccessAllowed(AppOpsManager.OP_WRITE_CLIPBOARD, callingPackage,
                            callingUid)) {
                    return;
                }
                checkDataOwnerLocked(clip, callingUid);
                final int userId = UserHandle.getUserId(callingUid);
                PerUserClipboard clipboard = getClipboard(userId);
                revokeUris(clipboard);
                setPrimaryClipInternal(clipboard, clip);
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
        @Override
        public ClipData getPrimaryClip(String pkg) {
            synchronized (this) {
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, pkg,
                            Binder.getCallingUid())) {
                    return null;
                }
                addActiveOwnerLocked(Binder.getCallingUid(), pkg);
                return getClipboard().primaryClip;
            }
        }

        @Override
        public ClipDescription getPrimaryClipDescription(String callingPackage) {
            synchronized (this) {
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return null;
                }
                PerUserClipboard clipboard = getClipboard();
                return clipboard.primaryClip != null ? clipboard.primaryClip.getDescription() : null;
            }
        }

        @Override
        public boolean hasPrimaryClip(String callingPackage) {
            synchronized (this) {
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return false;
                }
                return getClipboard().primaryClip != null;
            }
        }

        @Override
        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener,
                String callingPackage) {
            synchronized (this) {
                getClipboard().primaryClipListeners.register(listener,
                        new ListenerInfo(Binder.getCallingUid(), callingPackage));
            }
        }

        @Override
        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
            synchronized (this) {
                getClipboard().primaryClipListeners.unregister(listener);
            }
        }

        @Override
        public boolean hasClipboardText(String callingPackage) {
            synchronized (this) {
                if (!clipboardAccessAllowed(AppOpsManager.OP_READ_CLIPBOARD, callingPackage,
                            Binder.getCallingUid())) {
                    return false;
                }
                PerUserClipboard clipboard = getClipboard();
                if (clipboard.primaryClip != null) {
                    CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
                    return text != null && text.length() > 0;
                }
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
           2. uid if int type 

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
    private final void grantUriLocked(Uri uri, String pkg, int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            int sourceUserId = ContentProvider.getUserIdFromUri(uri, userId);
            uri = ContentProvider.getUriWithoutUserId(uri);
            mAm.grantUriPermissionFromOwner(mPermissionOwner, Process.myUid(), pkg,
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION, sourceUserId, userId);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void grantItemLocked(ClipData.Item item, String pkg, int userId) {
        if (item.getUri() != null) {
            grantUriLocked(item.getUri(), pkg, userId);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriLocked(intent.getData(), pkg, userId);
        }
    }

    private final void addActiveOwnerLocked(int uid, String pkg) {
        final IPackageManager pm = AppGlobals.getPackageManager();
        final int targetUserHandle = UserHandle.getCallingUserId();
        final long oldIdentity = Binder.clearCallingIdentity();
        try {
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
        PerUserClipboard clipboard = getClipboard();
        if (clipboard.primaryClip != null && !clipboard.activePermissionOwners.contains(pkg)) {
            final int N = clipboard.primaryClip.getItemCount();
            for (int i=0; i<N; i++) {
                grantItemLocked(clipboard.primaryClip.getItemAt(i), pkg, UserHandle.getUserId(uid));
            }
            clipboard.activePermissionOwners.add(pkg);
        }
    }
//*********************************************** BLOCK-10: BEGINS      Author: Shawn Bailey***********************************************************
    private final void revokeUriLocked(Uri uri) {
        int userId = ContentProvider.getUserIdFromUri(uri,
                UserHandle.getUserId(Binder.getCallingUid()));
        long ident = Binder.clearCallingIdentity();
        try {
            uri = ContentProvider.getUriWithoutUserId(uri);
            mAm.revokeUriPermissionFromOwner(mPermissionOwner, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    userId);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void revokeItemLocked(ClipData.Item item) {
        if (item.getUri() != null) {
            revokeUriLocked(item.getUri());
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            revokeUriLocked(intent.getData());
        }
    }

    private final void revokeUris(PerUserClipboard clipboard) {
        if (clipboard.primaryClip == null) {
            return;
        }
        final int N = clipboard.primaryClip.getItemCount();
        for (int i=0; i<N; i++) {
            revokeItemLocked(clipboard.primaryClip.getItemAt(i));
        }
    }

    private boolean clipboardAccessAllowed(int op, String callingPackage, int callingUid) {
        // Check the AppOp.
        if (mAppOps.checkOp(op, callingUid, callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }
        try {
            // Installed apps can access the clipboard at any time.
            if (!AppGlobals.getPackageManager().isInstantApp(callingPackage,
                        UserHandle.getUserId(callingUid))) {
                return true;
            }
            // Instant apps can only access the clipboard if they are in the foreground.
            return mAm.isAppForeground(callingUid);
        } catch (RemoteException e) {
            Slog.e("clipboard", "Failed to get Instant App status for package " + callingPackage,
                    e);
            return false;
        }
    }
}