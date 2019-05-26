/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_cheeses.*

class CheeseActivity : BaseSearchActivity() {

    override fun onStart() {
        super.onStart()

        // Create the Observable using the function you created
        val searchTextObservable = createButtonClickObservable()

        // Subscribe to the Observable with subscribe() and supply a simple Consumer
        searchTextObservable
                // The next operator in chain will be run on main thread
                .observeOn(AndroidSchedulers.mainThread())
                // showProgress() will be called every time a new item is emitted
                .doOnNext { showProgress() }
                // Next operator should be called on I/O thread
                .observeOn(Schedulers.io())
                // For each search query, return a list of results
                .map { cheeseSearchEngine.search(it) }
                // Results are passed to the list on main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideProgress()
                    showResult(it)
                }
    }

    // This function will return an Observable that will emit Strings
    private fun createButtonClickObservable(): Observable<String> {

        // Create the Observable and provide is an ObservableOnSubscribe
        return Observable.create { emitter ->

            // Setup the listener on the search button
            searchButton.setOnClickListener {

                // Pass the EditText content to the emitter
                emitter.onNext(queryEditText.text.toString())
            }

            // Will be called when Observable is disposed (i.e. Observable is completed or unsubscribed)
            emitter.setCancellable {
                searchButton.setOnClickListener(null)
            }
        }
    }
}