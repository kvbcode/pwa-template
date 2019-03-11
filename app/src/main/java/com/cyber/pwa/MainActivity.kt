package com.cyber.pwa

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.*
import java.io.IOException
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, WebViewFragment.OnPageDownloadListener {
    private val TAG = "PWA"
    private val PARAM_MENU_ITEM_ID = "menu_item_id"

    private val URL_MENU_DATA = "https://kvbcode.github.io/data/menu_data.json"
    private val httpClient = HttpClient.instance

    private lateinit var jsonMenu:JsonMenuAdapter
    private var tabMenuList:List<JsonMenuAdapter.TabItem> = emptyList()
    private var activeFragment:WebViewFragment? = null
    private var activeMenuItemId = -1

    private var DEFAULT_TITLE = "HOME"
    private var DEFAULT_URI = ""
    private var prefetch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        viewPager.addOnPageChangeListener(object:ViewPager.SimpleOnPageChangeListener(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (prefetch) {
                    val adapter = viewPager?.adapter
                    val pagesCount = adapter?.count ?: 0

                    if (position < pagesCount - 1) {
                        Log.v(TAG, "prefetch cached fragment for pos:${position + 1}")
                        adapter?.instantiateItem(viewPager, position + 1)
                    }
                }
            }
        })

        Log.v(TAG, "onCreate")

        loadNavigationMenuAsync(httpClient, URL_MENU_DATA)

        createNavigationMenu(null)
        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null){
            selectMenuItem(-1)
        }
    }

    fun loadNavigationMenuAsync(okHttpClient:OkHttpClient, url:String){
        val request = Request.Builder().url( url ).build()
        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "http error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful){
                    val jsonStr = response.body()?.string()
                    createNavigationMenu(jsonStr)
                }
            }
        })
    }

    fun createNavigationMenu(jsonStr:String?){
        val menu = nav_view.menu

        val jsonMenuData = if (jsonStr==null || jsonStr.isEmpty()){
            resources.openRawResource(R.raw.menu_data).bufferedReader().readText()
        }else{
            jsonStr
        }

        jsonMenu = JsonMenuAdapter(jsonMenuData)
        jsonMenu.inflate(menu)

        DEFAULT_URI = jsonMenu.defaultUri
    }

    fun makePagerAdapter():FragmentStatePagerAdapter {
        return object : FragmentStatePagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                val frag = WebViewFragment.newInstance(tabMenuList[position].uriStr)
                if (activeFragment == null || position == viewPager.currentItem) {
                    Log.v(TAG, "store activeFragment: $frag")
                    activeFragment = frag
                }
                return frag
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return tabMenuList[position].title
            }

            override fun getCount(): Int {
                return tabMenuList.size
            }

        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(PARAM_MENU_ITEM_ID, activeMenuItemId)
        Log.v(TAG, "save $PARAM_MENU_ITEM_ID=$activeMenuItemId, tab=${viewPager.currentItem}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        activeMenuItemId = savedInstanceState?.getInt(PARAM_MENU_ITEM_ID) ?: -1
        selectMenuItem(activeMenuItemId)
        Log.v(TAG, "load menu_item_id=$activeMenuItemId, tab=${viewPager.currentItem}")
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            if(activeFragment?.getWebView()?.canGoBack() == true) activeFragment?.getWebView()?.goBack()
        }
    }

    fun setTabList(tabList:List<JsonMenuAdapter.TabItem>){
        Log.v(TAG, "setTabList items: ${tabList.size}")

        tabMenuList = tabList
        val pagerAdapter = makePagerAdapter()
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)

        if (pagerAdapter.count>1){
            if (prefetch) pagerAdapter.instantiateItem(viewPager, 1)
            tabLayout.visibility = View.VISIBLE
        }else{
            tabLayout.visibility = View.GONE
        }
    }

    fun singleTabList(title:String?="", url:String?=DEFAULT_URI):List<JsonMenuAdapter.TabItem>{
        val tempList = ArrayList<JsonMenuAdapter.TabItem>()
        tempList.add( JsonMenuAdapter.TabItem(title ?: "", url ?: DEFAULT_URI ) )
        return tempList
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        selectMenuItem( item.itemId )
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun selectMenuItem(itemId:Int ){
        val menuItem = jsonMenu.getById( itemId )
        activeMenuItemId = itemId

        viewPager.currentItem = 0
        Log.v(TAG, "selectMenuItem: $itemId")
        setToolbarTitle( menuItem?.title ?: DEFAULT_TITLE )

        if (itemId==-1){
            setTabList( singleTabList( "HOME", DEFAULT_URI ) )
            loadUrl(DEFAULT_URI)
        }else{
            setTabList(menuItem?.tabs ?: singleTabList(menuItem?.title, menuItem?.uriStr))
            if (menuItem?.tabs?.isEmpty() == true) setTabList( singleTabList(menuItem?.title, menuItem?.uriStr) )
            loadUrl(menuItem?.uriStr)
        }
    }

    fun setToolbarTitle(title:String){
        toolbar.title = title
    }

    fun loadUrl(url:String?=DEFAULT_URI){
        Log.v(TAG, "loadUrl: $url")
        activeFragment?.getWebView()?.loadUrl(url ?: DEFAULT_URI)
    }

    override fun onPageDownloadStarted() {
        progressBar.visibility = View.VISIBLE
    }

    override fun onPageDownloadProgress(progress: Int) {
        progressBar.progress = progress
    }

    override fun onPageDownloadFinished() {
        progressBar.visibility = View.GONE
    }


}
