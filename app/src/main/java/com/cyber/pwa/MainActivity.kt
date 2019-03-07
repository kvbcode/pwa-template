package com.cyber.pwa

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, WebViewFragment.OnPageDownloadListener {
    private val TAG = "PWA"

    private lateinit var jsonMenu:JsonMenuAdapter
    private var tabMenuList:List<JsonMenuAdapter.TabItem> = emptyList()
    private var currentFragment:WebViewFragment? = null

    private var DEFAULT_URI = ""

    private val pagerAdapter = object: FragmentStatePagerAdapter(supportFragmentManager) {
        override fun getItem(position: Int): Fragment {
            val frag = WebViewFragment.newInstance( tabMenuList[position].uriStr )
            if (position==viewPager.currentItem) currentFragment = frag
            return frag
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return tabMenuList[position].title
        }

        override fun getCount(): Int {
            return tabMenuList.size
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

        createNavigationMenu()
        nav_view.setNavigationItemSelectedListener(this)

        setTabList( emptyList() )

        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)

        showSingleTab("HOME", DEFAULT_URI)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            if(currentFragment?.getWebView()?.canGoBack() ?: false) currentFragment?.getWebView()?.goBack()
        }
    }

    fun createNavigationMenu(){
        val menu = nav_view.menu

        var jsonStr = resources.openRawResource(R.raw.menu_data).bufferedReader().readText()
        jsonMenu = JsonMenuAdapter(jsonStr)
        jsonMenu.inflate(menu)

        DEFAULT_URI = jsonMenu.defaultUri
    }

    fun setTabList(tabList:List<JsonMenuAdapter.TabItem>){
        tabMenuList = tabList
        pagerAdapter.notifyDataSetChanged()

        when(tabLayout.tabCount>1){
            true -> tabLayout.visibility = View.VISIBLE
            else -> tabLayout.visibility = View.GONE
        }
    }

    fun showSingleTab(title:String?="", url:String?=DEFAULT_URI){
        val tempList = ArrayList<JsonMenuAdapter.TabItem>()
        tempList.add( JsonMenuAdapter.TabItem(title ?: "", url ?: DEFAULT_URI ) )
        setTabList( tempList )
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
        val menuItem = jsonMenu.getById( item.itemId )

        viewPager.setCurrentItem(0)
        setTabList(menuItem?.tabs ?: emptyList())
        loadUrl(menuItem?.uriStr)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun loadUrl(url:String?=DEFAULT_URI){
        Log.v(TAG, "loadUrl: $url")
        currentFragment?.getWebView()?.loadUrl(url ?: DEFAULT_URI)
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
