# Public
リソース名を自動で取得してそのリソース名に以下が含まれている場合削除されます。

将来的には、アプリにログが出力されるようにして、
そこで、リソース名を追加できるようにしたいです。

                        if (className.contains("Ad")
                                || className.contains("com.five_corp.ad.internal.view.d")
                                || className.equals("com.five_corp.ad.internal.view.m")
                                || className.contains("com.mbridge.msdk.videocommon.view.MyImageView")
                                || className.contains("com.mbridge.msdk.nativex.view.WindVaneWebViewForNV")
                                || resourceName.contains("Ad")
                                || resourceName.contains("adaptive_banner_container")
                                || resourceName.contains("footer_banner_ad_container")
                                || resourceName.contains("ad_label")
                                || resourceName.contains("layoutMediaContainer")
                                || resourceName.contains("adg_container")
                                || resourceName.contains("rect_banner_ad_container")
                                || resourceName.contains("ad_container")
                                || resourceName.contains("textAdLabel")
                                || resourceName.contains("mbridge")
                                || resourceName.contains("mbridge_ll_playerview_container")
                                || resourceName.contains("mbridge_my_big_img")
                                || resourceName.contains("mbridge_nativex_webview_layout_webview")
                                || resourceName.contains("mbridge_native_pb")
                                || resourceName.contains("mbridge_nativex_webview_layout")
                                || resourceName.contains("buttonRemoveAd")