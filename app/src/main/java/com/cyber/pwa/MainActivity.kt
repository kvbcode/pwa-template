package com.cyber.pwa

import android.graphics.Bitmap
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var jsonMenu:JsonMenuAdapter
    private var tabMenuList:List<JsonMenuAdapter.TabItem> = emptyList()
    private val TAG = "PWA"

    private var DEFAULT_URI = ""
    private var lastUriStr = ""

    inner class InnerWebViewClient: WebViewClient() {

        var jobProgressBarUpdate = setupProgressBarUpdate()

        fun setupProgressBarUpdate() = GlobalScope.async(Dispatchers.Main) {
            var endCounter = 5
            var prevValue = 0
            while( endCounter>0 ) {
                    val curValue = webView.progress
                    if (curValue==100 && curValue==prevValue) endCounter--
                    progressBar.progress = webView.progress
                    prevValue = webView.progress
                delay(200)
            }
            progressBar.visibility = View.GONE
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
            progressBar.visibility = View.VISIBLE
            if (!jobProgressBarUpdate.isActive){
                jobProgressBarUpdate = setupProgressBarUpdate()
                jobProgressBarUpdate.start()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = InnerWebViewClient()

        createNavigationMenu()
        nav_view.setNavigationItemSelectedListener(this)

        createTabs( emptyList() )
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) { }
            override fun onTabUnselected(tab: TabLayout.Tab?) { }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                var pos:Int = tab?.position ?: -1
                if (pos!=-1) loadUrl( tabMenuList[pos].uriStr )
            }
        })

        loadUrl("")
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        outState?.putString("last_uri", lastUriStr)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        webView.loadUrl( savedInstanceState?.getString("last_uri") )
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            if(webView.canGoBack()) webView.goBack()
        }
    }

    fun createNavigationMenu(){
        val menu = nav_view.menu

        var jsonStr = resources.openRawResource(R.raw.menu_data).bufferedReader().readText()
        jsonMenu = JsonMenuAdapter(jsonStr)
        jsonMenu.inflate(menu)

        DEFAULT_URI = jsonMenu.defaultUri
    }

    fun createTabs(tabList:List<JsonMenuAdapter.TabItem>){
        tabMenuList = tabList
        tabLayout.removeAllTabs()

        for(tab in tabList){
            tabLayout.addTab( tabLayout.newTab().setText( tab.title ) )
        }

        when(tabLayout.tabCount>0){
            true -> tabLayout.visibility = View.VISIBLE
            else -> tabLayout.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val menuItem = jsonMenu.getById( item.itemId )

        Log.v(TAG, "onNavigationItemSelected(): ${item.itemId}, uri: ${menuItem?.uriStr}")

        createTabs(menuItem?.tabs ?: emptyList())
        loadUrl( menuItem?.uriStr ?: "" )

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun loadUrl(url:String){

        lastUriStr = when(url.isEmpty()){
            true -> DEFAULT_URI
            else -> url
        }

        webView.loadUrl( lastUriStr )
    }

}
