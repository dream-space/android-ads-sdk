package dreamspace.ads.sdk.format;

import static dreamspace.ads.sdk.AdConfig.ad_admob_banner_unit_id;
import static dreamspace.ads.sdk.AdConfig.ad_applovin_banner_unit_id;
import static dreamspace.ads.sdk.AdConfig.ad_applovin_banner_zone_id;
import static dreamspace.ads.sdk.AdConfig.ad_fan_banner_unit_id;
import static dreamspace.ads.sdk.AdConfig.ad_manager_banner_unit_id;
import static dreamspace.ads.sdk.AdConfig.ad_networks;
import static dreamspace.ads.sdk.AdConfig.retry_from_start_max;
import static dreamspace.ads.sdk.data.AdNetworkType.ADMOB;
import static dreamspace.ads.sdk.data.AdNetworkType.APPLOVIN;
import static dreamspace.ads.sdk.data.AdNetworkType.APPLOVIN_DISCOVERY;
import static dreamspace.ads.sdk.data.AdNetworkType.APPLOVIN_MAX;
import static dreamspace.ads.sdk.data.AdNetworkType.FAN;
import static dreamspace.ads.sdk.data.AdNetworkType.FAN_BIDDING_ADMOB;
import static dreamspace.ads.sdk.data.AdNetworkType.FAN_BIDDING_AD_MANAGER;
import static dreamspace.ads.sdk.data.AdNetworkType.FAN_BIDDING_APPLOVIN_MAX;
import static dreamspace.ads.sdk.data.AdNetworkType.FAN_BIDDING_IRONSOURCE;
import static dreamspace.ads.sdk.data.AdNetworkType.IRONSOURCE;
import static dreamspace.ads.sdk.data.AdNetworkType.MANAGER;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.applovin.adview.AppLovinAdView;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkUtils;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdView;

import java.util.List;

import dreamspace.ads.sdk.AdNetwork;
import dreamspace.ads.sdk.data.AdNetworkType;
import dreamspace.ads.sdk.helper.AppLovinCustomEventBanner;
import dreamspace.ads.sdk.utils.Tools;

public class BannerAdFormat {

    private static final String TAG = AdNetwork.class.getSimpleName();

    private final Activity activity;
    private LinearLayout adContainer;

    public BannerAdFormat(Activity activity) {
        this.activity = activity;
    }

    public void loadBannerAdMain(int ad_index, int retry_count, LinearLayout ad_container) {
        if (retry_count > retry_from_start_max) return;

        ad_container.setVisibility(View.GONE);
        ad_container.removeAllViews();
        AdNetworkType type = ad_networks[ad_index];
        ad_container.post(() -> {
            if (type == ADMOB || type == FAN_BIDDING_ADMOB) {
                AdView adView = new AdView(activity);
                adView.setAdUnitId(ad_admob_banner_unit_id);
                ad_container.addView(adView);
                adView.setAdSize(Tools.getAdSize(activity));
                adView.loadAd(Tools.getAdRequest(activity));
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, type.name() + " banner onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, type.name() + " banner onAdFailedToLoad : " + adError.getMessage());
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }
                });
            } else if (type == MANAGER || type == FAN_BIDDING_AD_MANAGER) {
                AdManagerAdView adView = new AdManagerAdView(activity);
                adView.setAdUnitId(ad_manager_banner_unit_id);
                ad_container.addView(adView);
                adView.setAdSize(Tools.getAdSize(activity));
                adView.loadAd(Tools.getGoogleAdManagerRequest());

                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.d(TAG, type.name() + " banner onAdFailedToLoad : " + loadAdError.getMessage());
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        Log.d(TAG, "MANAGER onAdLoaded");
                        ad_container.setVisibility(View.VISIBLE);
                    }
                });
            } else if (type == FAN) {
                com.facebook.ads.AdView adView = new com.facebook.ads.AdView(activity, ad_fan_banner_unit_id, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
                // Add the ad view to your activity layout
                ad_container.addView(adView);
                com.facebook.ads.AdListener adListener = new com.facebook.ads.AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, type.name() + " banner onAdFailedToLoad : " + adError.getErrorMessage());
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, type.name() + " banner onAdLoaded");
                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                };
                com.facebook.ads.AdView.AdViewLoadConfig loadAdConfig = adView.buildLoadAdConfig().withAdListener(adListener).build();
                adView.loadAd(loadAdConfig);

            } else if (type == APPLOVIN || type == APPLOVIN_MAX || type == FAN_BIDDING_APPLOVIN_MAX) {
                Log.d(TAG, type.name() + " loadBannerAdMain");
                MaxAdView maxAdView = new MaxAdView(ad_applovin_banner_unit_id, activity);
                maxAdView.setListener(new MaxAdViewAdListener() {
                    @Override
                    public void onAdExpanded(MaxAd ad) {

                    }

                    @Override
                    public void onAdCollapsed(MaxAd ad) {

                    }

                    @Override
                    public void onAdLoaded(MaxAd ad) {
                        Log.d(TAG, type.name() + " onBannerAdLoaded");
                        ad_container.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAdDisplayed(MaxAd ad) {

                    }

                    @Override
                    public void onAdHidden(MaxAd ad) {

                    }

                    @Override
                    public void onAdClicked(MaxAd ad) {

                    }

                    @Override
                    public void onAdLoadFailed(String adUnitId, MaxError error) {
                        Log.d(TAG, type.name() + " onAdLoadFailed " + error.getMessage());
                        ad_container.setVisibility(View.GONE);
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }

                    @Override
                    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                        Log.d(TAG, type.name() + " onAdDisplayFailed " + error.getMessage());
                        ad_container.setVisibility(View.GONE);
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }
                });

                int width = ViewGroup.LayoutParams.MATCH_PARENT;
                int heightPx = Tools.dpToPx(activity, 50);
                maxAdView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
                ad_container.addView(maxAdView);
                maxAdView.loadAd();
            } else if (type == APPLOVIN_DISCOVERY) {
                AdRequest.Builder builder = new AdRequest.Builder();
                Bundle bannerExtras = new Bundle();
                bannerExtras.putString("zone_id", ad_applovin_banner_zone_id);
                builder.addCustomEventExtrasBundle(AppLovinCustomEventBanner.class, bannerExtras);

                boolean isTablet2 = AppLovinSdkUtils.isTablet(activity);
                AppLovinAdSize adSize = isTablet2 ? AppLovinAdSize.LEADER : AppLovinAdSize.BANNER;
                AppLovinAdView adView = new AppLovinAdView(adSize, activity);
                adView.setAdLoadListener(new AppLovinAdLoadListener() {
                    @Override
                    public void adReceived(AppLovinAd ad) {
                        ad_container.setVisibility(View.VISIBLE);
                        Log.d(TAG, type.name() + " adReceived");
                    }

                    @Override
                    public void failedToReceiveAd(int errorCode) {
                        ad_container.setVisibility(View.GONE);
                        Log.d(TAG, type.name() + " failedToReceiveAd : " + errorCode);
                        retryLoadBanner(ad_index, retry_count, ad_container);
                    }
                });
                ad_container.addView(adView);
                adView.loadNextAd();
            }
        });

        adContainer = ad_container;
    }

    private void retryLoadBanner(int ad_index, int retry_count, LinearLayout ad_container) {
        int adIndex = ad_index + 1;
        int finalRetry = retry_count;
        if (adIndex > ad_networks.length - 1) {
            adIndex = 0;
            finalRetry++;
        }
        final int _adIndex = adIndex, _finalRetry = finalRetry;
        Log.d(TAG, "delayAndLoadBanner ad_index : " + _adIndex + " retry_count : " + _finalRetry);
        new Handler(activity.getMainLooper()).postDelayed(() -> {
            loadBannerAdMain(_adIndex, _finalRetry, ad_container);
        }, 3000);
    }


    public void destroyAndDetachBanner(List<AdNetworkType> adNetworks) {

    }

}
