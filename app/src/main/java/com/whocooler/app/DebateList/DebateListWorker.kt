package com.whocooler.app.DebateList

import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.whocooler.app.Common.App.App
import com.whocooler.app.Common.Models.DebateVoteResponse
import com.whocooler.app.Common.Models.DebatesResponse
import com.whocooler.app.Common.Utilities.BASE_URL
import com.google.gson.Gson
import io.reactivex.rxjava3.subjects.PublishSubject
import org.json.JSONObject

class DebateListWorker {

    fun getDebates(page: Int, categoryId: String? = "all", sorting: String) : PublishSubject<DebatesResponse> {
        val responseSubject = PublishSubject.create<DebatesResponse>()

        val debatesRequest = object : JsonObjectRequest(Method.GET, BASE_URL + "en/debates" + "?category_id=$categoryId&sorting=$sorting", null, Response.Listener {response ->
            val debatesResponse = Gson().fromJson(response.toString(), DebatesResponse :: class.java)

            responseSubject.onNext(debatesResponse)
        }, Response.ErrorListener {
            Log.d("RESPONSE ERROR ", "Error" + it.localizedMessage)
        }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                var token = App.prefs.userSession?.accessToken
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token)
                }
                return headers
            }

        }

        App.prefs.requestQueue.add(debatesRequest)

        return responseSubject
    }

    fun vote(debateId: String, sideId: String) : PublishSubject<DebateVoteResponse> {
        val responseSubject = PublishSubject.create<DebateVoteResponse>()

        val jsonBody = JSONObject()
        jsonBody.put("debate_id", debateId)
        jsonBody.put("side_id", sideId)
        val requestBody = jsonBody.toString()

        val voteRequest = object : JsonObjectRequest(Method.POST, BASE_URL + "vote", null, Response.Listener {response ->
            val debateVoteResponse = Gson().fromJson(response.toString(), DebateVoteResponse :: class.java)

            responseSubject.onNext(debateVoteResponse)
        }, Response.ErrorListener {
            Log.d("?!?!RESPONSE ERROR ", "VOTE ERROR" + it.networkResponse + it.localizedMessage)
        }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                var token = App.prefs.userSession?.accessToken
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token)
                }
                return headers
            }
        }

        App.prefs.requestQueue.add(voteRequest)

        return responseSubject
    }

}