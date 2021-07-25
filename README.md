 1. ## 背景
因工作需要，想要在提供一种能力，供三方apk进行调用，通常的解决方式就是通过提供aar给到三方apk进行继承。但是这样的方式就会有一个明显的缺陷，就是在资源文件众多的时候，aar文件会变得非常大，进而导致三方apk的包体变大。这样显然是不利于三方进行接入，并且在多个apk集成后，同样的内容在同一个系统中打包了多份，这显然也不是我们想要的。这个时候，shared-library就十分符合我们的诉求。
网上搜索了半天，关于sharedlibrary实在是少之又少，因此经过一番周折以后，我觉得有必要为后续碰到问题的人提供一点帮助。
接下来我会一步一步得介绍如何创建一个自己得共享库。
 2. ## 使用共享库
这个其实就比较简单，直接看官方文档就可以，放上官方文档就不再进行详细解释。
https://developer.android.google.cn/guide/topics/manifest/uses-library-element?hl=en
 3. ## 创建一个空项目
如图，为了保险起见，我们把所有的资源文件也全给删了
![这是一个空项目](https://img-blog.csdnimg.cn/20210711202508892.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTI4ODAzMzg=,size_16,color_FFFFFF,t_70)
然后，我们先创建一个普通的类，只有一个测试方法

```java
public class LibMain {
    public int getMyNumber(int a) {
        return a * a;
    }
}
```

 4. ## 编译为系统共享库
AndroidStudio使用aapt进行编译，aapt可以添加对应参数表明当前项目需要编译为系统共享库，此处是**重点**，考试会考，基本是本文最主要的部分。添加**系统共享库编译参数**

```java
aaptOptions {
        aaptOptions.additionalParameters("--shared-lib")
    }
```

 5. ## manifest中声明
在项目的AndroidManifest.xml中添加library声明，添加至application中间，如下

```xml
<application
    android:allowBackup="true"
    android:supportsRtl="true">
    <library android:name="com.izhangqian.mysharedlib" />
</application>
```

 6. ## 编译成apk使用
到这一步，我们这个共享库库基本就已制作完成了。编译这里也没什么好说的，就是保证代码不报错，正常编译就可以了。
不过这里要说明的一点是，这个apk必须要通过push到系统目录由系统进行预加载，所以普通第三方很难用这个方式作为一个公共的库。
我大部分是通过实机进行测试的，在写这篇文章时，为了更加具备通用性，便采用了虚拟机，这里要吐槽一下谷歌可太坏了，安卓11的虚拟机system目录,product目录都不是可写的，查找各种说法，没一个在我这行得通的，如有大佬知晓如何挂载，还请指教。我最后选择了一个API25的虚拟机进行调试和测试。
PS:此处启动虚拟机需用命令，进入androidsdk emulator目录，使用emulator启动 emulator @avdname -writable-system 或 emulator -avd avdname -writable-system.如下：

```powershell
emulator @Pixelxl -writable-system
```

 7. ## 使用
关于这个使用，其实上面文档里基本都提了，不过你要是想要调用方直接能够编译，就需要把这个apk编到他对应的androidsdk里，不过我这里为了省事，就直接让调用方通过反射的方式进行调用了。
 8. ## 发现问题
添加资源文件后编译，并在代码中进行调用
```java
public String getStringById(String id) {
	return getContext().getString(R.string.app_name);
}
```
发现并无法使用资源。在方法中执行报错。没有找到对应资源。
此处略去查找各种资料的步骤，解决方案就是需要把gradle 插件版本改为3.0.1，gradle版本设置为4.1，如下：

```java
// 这是在项目下的build.gradle
classpath "com.android.tools.build:gradle:3.0.1"
// 这是在gradle目录下的gradle.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-4.1-all.zip

```
我目前只在这两个版本中试成功，原因就是我们前面加的aapt的参数，在更高的版本中不生效，如果有更高版本的方式，欢迎在评论区告知一下。不生效以后就会导致生成的R文件生成的资源id跟普通应用一样是final的，而预期的R文件应该是如下的（可以用生成的apk反编译等各种方式查看）

```java
package com.izhangqian.mysharedlib;

public final class R {

    public static final class layout {
        public static int test_layout = test_layout;
    }

    public static final class string {
        public static int app_name = app_name;
    }

    public static void onResourcesLoaded(int p) {
        R.layout.test_layout = (R.layout.test_layout & 16777215) | (p << 24);
        R.string.app_name = (R.string.app_name & 16777215) | (p << 24);
    }

```
经过上面gradle环境的修改配置生成的apk基本上就没问题了。
PS：如果你用的Android Studio版本较新，无法使用gradle4.1,可以下载choose runtime插件进行修改，使用传送门https://www.jianshu.com/p/e8b616731af9

 9. ## 其他问题
 到这里我们的共享库基本就建设完成了，但是还有一个点，这个apk中尽量不要导入和使用其他的库，包括RecyclerView，会导致宿主apk找不到对应的类。关于这点，我并没有去深究，应该是跟类加载有关，怀疑是不是加载库apk时是由应用父加载器加载了对应的类，导致宿主apk中无法继续加载，但是父加载器又对这些类增加了权限什么的，导致应用的加载器无法加载，实际原因就没有深究了。如果有人看到这篇文档，并明白其中的原因，也可以告知一下。
本文中项目地址：
https://github.com/xtayfjtn/MySharedLib
