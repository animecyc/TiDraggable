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

#import "TiDraggableGesture.h"
#import "TiViewProxy.h"
#import "TiUtils.h"

@implementation TiDraggableGesture

-(id)initWithView:(TiUIView*)view andOptions:(NSDictionary *)options
{
    if (self = [super init])
    {
        _view = view;
        _options = [options retain];
        
        UIPanGestureRecognizer* recognizer = [[[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(panDetected:)] autorelease];
        
        [_view addGestureRecognizer:recognizer];
    }
    
    return self;
}

- (void)panDetected:(UIPanGestureRecognizer *)panRecognizer
{
    ENSURE_UI_THREAD_1_ARG(panRecognizer);
    
    CGPoint translation = [panRecognizer translationInView:_view];
    CGPoint imageViewPosition = _view.center;
    
    NSString* axis = [_options objectForKey:@"axis"];
    float maxLeft = [[_options objectForKey:@"maxLeft"] floatValue];
    float minLeft = [[_options objectForKey:@"minLeft"] floatValue];
    float maxTop = [[_options objectForKey:@"maxTop"] floatValue];
    float minTop = [[_options objectForKey:@"minTop"] floatValue];
    
    BOOL hasMaxLeft = [_options objectForKey:@"maxLeft"] != nil;
    BOOL hasMinLeft = [_options objectForKey:@"minLeft"] != nil;
    BOOL hasMaxTop = [_options objectForKey:@"maxTop"] != nil;
    BOOL hasMinTop = [_options objectForKey:@"minTop"] != nil;
    
    BOOL ensureRight = [TiUtils boolValue:[_options objectForKey:@"ensureRight"] def:NO];
    BOOL ensureBottom = [TiUtils boolValue:[_options objectForKey:@"ensureBottom"] def:NO];
    
    if([axis isEqualToString:@"x"])
    {
        imageViewPosition.x += translation.x;
        imageViewPosition.y = imageViewPosition.y;
    }
    else if([axis isEqualToString:@"y"])
    {
        imageViewPosition.x = imageViewPosition.x;
        imageViewPosition.y += translation.y;
    }
    else
    {
        imageViewPosition.x += translation.x;
        imageViewPosition.y += translation.y;
    }
    
    if(hasMaxLeft || hasMaxTop || hasMinLeft || hasMinTop)
    {
        CGSize size = _view.frame.size;
        
        if(hasMaxLeft && imageViewPosition.x - size.width / 2 > maxLeft)
        {
            imageViewPosition.x = maxLeft + size.width / 2;
        }
        else if(hasMinLeft && imageViewPosition.x - size.width / 2 < minLeft)
        {
            imageViewPosition.x = minLeft + size.width / 2;
        }
        
        if(hasMaxTop && imageViewPosition.y - size.height / 2 > maxTop)
        {
            imageViewPosition.y = maxTop + size.height / 2;
        }
        else if(hasMinTop && imageViewPosition.y - size.height / 2 < minTop)
        {
            imageViewPosition.y = minTop + size.height / 2;
        }
    }
    
    _view.center = imageViewPosition;
    
    [panRecognizer setTranslation:CGPointZero inView:_view];
    
    float left = _view.frame.origin.x;
    float top = _view.frame.origin.y;
    
    NSDictionary *tiProps = [NSDictionary dictionaryWithObjectsAndKeys:
                             [NSNumber numberWithFloat:left], @"left",
                             [NSNumber numberWithFloat:top], @"top",
                             [[[TiPoint alloc] initWithPoint:_view.center] autorelease], @"center",
                             nil];
    
    [(TiViewProxy*)[_view proxy] setTop:[NSNumber numberWithFloat:top]];
    [(TiViewProxy*)[_view proxy] setLeft:[NSNumber numberWithFloat:left]];
    
    if (ensureRight)
    {
        [(TiViewProxy*)[_view proxy] setRight:[NSNumber numberWithFloat:-left]];
    }
    
    if (ensureBottom)
    {
        [(TiViewProxy*)[_view proxy] setBottom:[NSNumber numberWithFloat:-top]];
    }
    
    if([_view.proxy _hasListeners:@"start"] && [panRecognizer state] == UIGestureRecognizerStateBegan)
    {
        [_view.proxy fireEvent:@"start" withObject:tiProps];
    }
    else if([_view.proxy _hasListeners:@"move"] && [panRecognizer state] == UIGestureRecognizerStateChanged)
    {
        [_view.proxy fireEvent:@"move" withObject:tiProps];
    }
    else if([_view.proxy _hasListeners:@"end"] && [panRecognizer state] == UIGestureRecognizerStateEnded)
    {
        [_view.proxy fireEvent:@"end" withObject:tiProps];
    }
    else if([_view.proxy _hasListeners:@"cancel"] && [panRecognizer state] == UIGestureRecognizerStateCancelled)
    {
        [_view.proxy fireEvent:@"cancel" withObject:tiProps];
    }
    
}

-(void)dealloc
{
    RELEASE_TO_NIL(_options);
    
    [super dealloc];
}

@end