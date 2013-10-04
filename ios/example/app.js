/*global require,console,Ti*/
/*jslint devel: true, forin: true */

/**
 * An enhanced fork of the original TiDraggable module by Pedro Enrique,
 * allows for simple creation of "draggable" views.
 *
 * Copyright (C) 2013 Seth Benjamin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * -- Original License --
 *
 * Copyright 2012 Pedro Enrique
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    'use strict';

    var Draggable = require('ti.draggable'),
        mainWindow = Ti.UI.createWindow({
            backgroundColor : 'white',
            exitOnClose : true
        }),
        subscribe = function (proxy, observer) {
            var key, events, eIndex;

            for (key in observer) {
                if (typeof observer[key] === 'function') {
                    events = key.split(' ');

                    for (eIndex in events) {
                        proxy.addEventListener(events[eIndex], observer[key]);
                    }
                }
            }
        },
        createDraggableSquare = function (name, color, axis) {
            var view = Draggable.createView({
                    width : 100,
                    height : 100,
                    axis : axis,
                    borderRadius : 3,
                    minLeft : 0,
                    maxLeft : Ti.Platform.displayCaps.platformWidth - 100,
                    minTop : 0,
                    maxTop : Ti.Platform.displayCaps.platformHeight - 120,
                    backgroundColor : color || 'black'
                });

            view.add(Ti.UI.createLabel({
                text : name
            }));

            subscribe(view, {
                'start move end cancel' : function (e) {
                    console.log(
                        'Event: ' + e.type,
                        'Left: ' + e.left,
                        'Top: ' + e.top
                    );
                }
            });

            return view;
        },
        createNavigationWindow = function() {
            var navWindow = Ti.UI.iOS.createNavigationWindow({
                window : Ti.UI.createWindow({
                    backgroundColor : 'orange'
                })
            });

            navWindow.open();

            navWindow.openWindow(Ti.UI.createWindow({
                backgroundColor: 'blue'
            }));

            subscribe(navWindow, {
                'start move end cancel' : function (e) {
                    console.log(
                        'Event: ' + e.type,
                        'Left: ' + e.left,
                        'Top: ' + e.top
                    );
                }
            });

            return navWindow;
        };

    mainWindow.add(createDraggableSquare('Horizontal', 'red', 'x'));
    mainWindow.add(createDraggableSquare('Vertical', 'blue', 'y'));
    mainWindow.add(createDraggableSquare('Free', 'green'));

    mainWindow.open();

    Draggable.makeDraggable(createNavigationWindow(), {
        axis: 'x',
        minLeft : 0,
        maxLeft : 150,
        ensureRight: true
    });

}());