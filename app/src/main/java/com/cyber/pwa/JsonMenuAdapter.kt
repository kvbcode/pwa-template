package com.cyber.pwa

import android.net.Uri
import android.view.Menu
import org.json.JSONObject

class JsonMenuAdapter( jsonData:String ){

    class MenuItem(
        var id:Int,
        var title:String = "",
        var uriStr:String = ""

    )

    class MenuGroup(
        var title:String = "",
        var items:List<MenuItem>
    )

    var nextId = 0
    var defaultUri = ""
    var groupList:List<MenuGroup>

    init{
        val jsonRootObj = JSONObject(jsonData)
        groupList = parseRoot(jsonRootObj)
    }

    private fun parseRoot(json:JSONObject):List<MenuGroup>{
        var mutGroupList = ArrayList<MenuGroup>()

        defaultUri = json.getString("default_uri")
        var groupsObjArr = json.getJSONArray("groups")

        for(groupIndex in 0 until groupsObjArr.length()){
            var groupObj = groupsObjArr.getJSONObject(groupIndex)
            mutGroupList.add( parseMenuGroup( groupObj ) )
        }

        return mutGroupList;
    }

    private fun parseMenuGroup(json:JSONObject):MenuGroup{
        var mutItemsList = ArrayList<MenuItem>()

        var itemsObjArr = json.getJSONArray("items")

        for(itemIndex in 0 until itemsObjArr.length()){
            var itemObj = itemsObjArr.getJSONObject( itemIndex )
            mutItemsList.add( parseMenuItem( itemObj ) )
        }

        return MenuGroup(
            title = json.getString("group_title"),
            items = mutItemsList
        )
    }

    private fun parseMenuItem(json:JSONObject):MenuItem{
        return MenuItem(
            id = nextId++,
            title = json.getString("item_title"),
            uriStr =  json.getString( "uri" )
        )
    }

    fun inflate(menu: Menu){

        for(group in groupList){
            var subMenu = menu.addSubMenu( group.title )

            for(item in group.items){
                subMenu.add( Menu.NONE, item.id, Menu.NONE, item.title )
            }
        }
    }

    fun getById(id:Int):MenuItem?{
        for(group in groupList){
            for (menuItem in group.items){
                if (id==menuItem.id) return menuItem
            }
        }
        return null
    }
}