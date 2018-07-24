package cordova.plugin.chief.satnav;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class Satnav extends CordovaPlugin {
  public static String TAG="cs";
	  private Context mContext;
    private String address;
    private String lat,lng;

    private CallbackContext callbackContext;


	@Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);
      mContext=cordova.getActivity();
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext=callbackContext;
        if (action.equals("showMap")) {
          jsonInfo(args);
          return true;
        }
        return false;
    }


  private void jsonInfo(JSONArray jsonArray){
    try {
      JSONObject jsonarray = new JSONObject(jsonArray.getString(0));
      address=jsonarray.getString("target");
      lat=jsonarray.getString("lat");
      lng=jsonarray.getString("log");
      if (lat==null||lng==null){
        Toast.makeText(mContext,"未获取到经纬度",Toast.LENGTH_SHORT).show();
        return;
      }
      showDialog();
    }catch (JSONException e){
      Toast.makeText(mContext,"数据解析失败",Toast.LENGTH_SHORT).show();
    }

  }


  private Button bntBaidu,bntGaoDe,bntDialogCancel;
  private Dialog dialog;
  public void showDialog() {
    if (dialog != null && dialog.isShowing()){
      return;
    }
    LayoutInflater inflater=LayoutInflater.from(mContext);
   	View view=inflater.inflate(mContext.getResources().getIdentifier("layout_view_dialog_map","layout",mContext.getPackageName()),null);
	  bntBaidu=(Button)view.findViewById(mContext.getResources().getIdentifier("bntBaidu","id",mContext.getPackageName()));
    bntBaidu.setOnClickListener(new bntBaiDuOnclick());
    bntGaoDe=(Button)view.findViewById(mContext.getResources().getIdentifier("bntGaoDe","id",mContext.getPackageName()));
    bntGaoDe.setOnClickListener(new bntGaoDeOnclick());
    bntDialogCancel=(Button)view.findViewById(mContext.getResources().getIdentifier("bntDialogCancel","id",mContext.getPackageName()));
    bntDialogCancel.setOnClickListener(new bntDialogCancelOnclick());

    dialog = new Dialog(mContext,mContext.getResources().getIdentifier("transparentFrameWindowStyle","style",mContext.getPackageName()));
    dialog.setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT));
    Window window = dialog.getWindow();
    // 设置显示动画
    window.setWindowAnimations(mContext.getResources().getIdentifier("main_menu_animstyle","style",mContext.getPackageName()));
    WindowManager.LayoutParams wl = window.getAttributes();
    wl.x = 0;
    wl.y = window.getWindowManager().getDefaultDisplay().getHeight();
    // 保证按钮可以水平满屏
    wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
    wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    // 设置显示位置
    dialog.onWindowAttributesChanged(wl);
    // 设置点击外围解散
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
  }

   //百度
   class bntBaiDuOnclick implements View.OnClickListener{
    @Override
    public void onClick(View view) {
      openBaiDuMap();
      if (dialog!=null && dialog.isShowing()){
        dialog.dismiss();
      }
    }
  }
  //高德
  class bntGaoDeOnclick implements View.OnClickListener{
    @Override
    public void onClick(View view) {
      openGaoDeMap();
      if (dialog!=null && dialog.isShowing()){
        dialog.dismiss();
      }

    }
  }
  //取消
  class bntDialogCancelOnclick implements View.OnClickListener{
    @Override
    public void onClick(View view) {
      if (dialog!=null && dialog.isShowing()){
        dialog.dismiss();
      }
    }
  }






  private String  mode="driving";                 //驾车
  private String  coord_type="bd09ll"; //编码类型
  private String  region="";           //默认当前城市    参数：广州
  /**
   * 打开百度地图
   * 手机百度，如果是平板百度包名不同
   */
  private void openBaiDuMap(){
    //手机百度
    if (isAvilible(mContext,"com.baidu.BaiduMap")){
      try {
        Intent intent = Intent.getIntent("intent://map/direction?destination=name:"+ address +"|latlng:"+lat+","+lng+ "&coord_type=bd09ll&mode=navigation#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end");
        mContext.startActivity(intent);

      } catch (URISyntaxException e) {
        Toast.makeText(mContext,"未檢測到地址",Toast.LENGTH_SHORT).show();
        e.printStackTrace();
      }
    }else{
      Toast.makeText(mContext,"您还未安装百度地图",Toast.LENGTH_SHORT).show();
//      try {
//        Toast.makeText(mContext,"您还未安装百度地图",Toast.LENGTH_SHORT).show();
//        //显示手机上所有的market商店 可使用百度官网访问  此处是谷歌应用商店
//        Uri uri = Uri.parse("market://details?id=com.baidu.BaiduMap");
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        mContext.startActivity(intent);
//      }catch (Exception e){
//        Toast.makeText(mContext,"请手动安装",Toast.LENGTH_SHORT).show();
//      }
    }
  }



  private String poiName;//非必填
  private int  dev=0;    //是否偏移   必填
  private int  style=0; //导航方式
  private double gpg[];
  /**
   * 打开高德地图
   * 1.使用的是百度经纬度 需要转换为高德的使用
   */
  private void openGaoDeMap(){
    gpg=bd09_To_Gcj02(Double.parseDouble(lat),Double.parseDouble(lng));
    gpg=bd09_To_Gcj02(Double.parseDouble(lat),Double.parseDouble(lng));

    if (isAvilible(mContext,"com.autonavi.minimap")){
      gaodeGoToNaviActivity(mContext, getApplicationName(mContext), poiName,gpg[1],gpg[0],dev, style);
    }else{
      Toast.makeText(mContext,"您还未安装高德地图",Toast.LENGTH_SHORT).show();
//      Uri uri = Uri.parse("market://details?id=com.autonavi.minimap");
//      Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//      mContext.startActivity(intent);
    }
  }


    /**
   * 检查手机上是否安装了指定的软件
   * @param context
   * @param packageName：应用包名
   * @return
   */
  public static boolean isAvilible(Context context, String packageName){
    //获取packagemanager
    final PackageManager packageManager = context.getPackageManager();
    //获取所有已安装程序的包信息
    List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
    //用于存储所有已安装程序的包名
    List<String> packageNames = new ArrayList<String>();
    //从pinfo中将包名字逐一取出，压入pName list中
    if(packageInfos != null){
      for(int i = 0; i < packageInfos.size(); i++){
        String packName = packageInfos.get(i).packageName;
        packageNames.add(packName);
      }
    }
    //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
    return packageNames.contains(packageName);
  }

  /**
   * 获取应用名称
   * @param context
   * @return
     */
  public static  String getApplicationName(Context context) {
    PackageManager packageManager = null;
    ApplicationInfo applicationInfo = null;
    try {
      packageManager = context.getApplicationContext().getPackageManager();
      applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException e) {
      applicationInfo = null;
    }
    String applicationName =
      (String) packageManager.getApplicationLabel(applicationInfo);
    return applicationName;
  }

   /**
   * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
   * bd_lat * @param bd_lon * @return
   */
  private  double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
  private  double[] bd09_To_Gcj02( double lat,double lon) {
    double y = lat - 0.006;
    double x = lon - 0.0065;
    double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
    double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
    double tempLon = z * Math.cos(theta);
    double tempLat = z * Math.sin(theta);
    double[] gps = {tempLat,tempLon};
    return gps;
  }


  /**
   * @param sourceApplication 必填 第三方调用应用名称。如 amap
   * @param poiname 非必填 POI 名称
   *  @param lon 必填 经度
   * @param lat 必填 纬度
   * @param dev 必填 是否偏移(0:lat 和 lon 是已经加密后的,不需要国测加密; 1:需要国测加密)
   * @param style 必填 导航方式(0 速度快; 1 费用少; 2 路程短; 3 不走高速；4 躲避拥堵；5 不走高速且避免收费；6 不走高速且躲避拥堵；7 躲避收费和拥堵；8 不走高速躲避收费和拥堵))
   * @param context
   * @param sourceApplication
   * @param poiname
   * @param lat
   * @param lon
   * @param dev
   * @param style
   */
  public  void gaodeGoToNaviActivity(Context context,
                                            String sourceApplication ,
                                            String poiname ,
                                            double lon ,
                                            double lat ,
                                            int dev ,
                                            int style){
    StringBuffer stringBuffer  = new StringBuffer("androidamap://navi?sourceApplication=")
      .append(sourceApplication);
    if (!TextUtils.isEmpty(poiname)){
      stringBuffer.append("&poiname=").append(poiname);
    }
    stringBuffer.append("&lat=").append(lat)
      .append("&lon=").append(lon)
      .append("&dev=").append(dev)
      .append("&style=").append(style);
    try {
      Intent intent = new Intent("android.intent.action.VIEW", android.net.Uri.parse(stringBuffer.toString()));
      intent.setPackage("com.autonavi.minimap");
      context.startActivity(intent);
    }catch (Exception e){
      Toast.makeText(mContext,"未檢測到地址",Toast.LENGTH_SHORT).show();
    }

  }



  @Override
  public void onDestroy() {
    super.onDestroy();
  }


}
