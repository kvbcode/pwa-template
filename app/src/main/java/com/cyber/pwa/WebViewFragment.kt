package com.cyber.pwa

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.fragment_web_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


private const val PARAM_URL = "url"

class WebViewFragment : Fragment() {

    private var url: String = ""
    private var listener: OnPageDownloadListener? = null
    private val webViewClient = InnerWebViewClient()
    private var progressBar:ProgressBar? = null

    companion object {
        @JvmStatic fun newInstance(url: String) =
                WebViewFragment().apply {
                    arguments = Bundle().apply {
                        putString(PARAM_URL, url)
                    }
                }
    }

    interface OnPageDownloadListener {
        fun onPageDownloadStarted()

        fun onPageDownloadProgress(progress:Int)

        fun onPageDownloadFinished()
    }

    inner class InnerWebViewClient: WebViewClient() {

        var jobProgressBarUpdate = setupProgressBarUpdate()

        fun setupProgressBarUpdate() = GlobalScope.async(Dispatchers.Main) {
            var endCounter = 5
            var prevValue = 0
            while( endCounter>0 ) {
                val curValue = webView.progress
                if (curValue==100 && curValue==prevValue) endCounter--
                listener?.onPageDownloadProgress( webView.progress )
                prevValue = webView.progress
                delay(200)
            }
            listener?.onPageDownloadFinished()
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            view?.loadUrl(url)
            return true
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            view?.loadUrl(request?.toString())
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (listener==null) return

            listener?.onPageDownloadStarted()
            if (!jobProgressBarUpdate.isActive){
                jobProgressBarUpdate = setupProgressBarUpdate()
                jobProgressBarUpdate.start()
            }
        }

        fun stopUpdate(){
            listener?.onPageDownloadFinished()
            jobProgressBarUpdate.cancel()
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(PARAM_URL)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        webView.settings.javaScriptEnabled = true
        webView.webViewClient = webViewClient
        webView.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        webViewClient.stopUpdate()
        progressBar = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPageDownloadListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    public fun getWebView():WebView?{
        return webView
    }

}
