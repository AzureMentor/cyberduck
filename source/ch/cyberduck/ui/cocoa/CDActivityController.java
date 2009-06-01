package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.AbstractCollectionListener;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.NSNotification;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.threading.BackgroundAction;
import ch.cyberduck.ui.cocoa.threading.BackgroundActionRegistry;
import ch.cyberduck.ui.cocoa.threading.WindowMainAction;

import org.apache.log4j.Logger;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.CGFloat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version $Id$
 */
public class CDActivityController extends CDWindowController implements CDListDataSource {
    private static Logger log = Logger.getLogger(CDActivityController.class);

    private static CDActivityController instance;

    public static CDActivityController instance() {
        synchronized(NSApplication.sharedApplication()) {
            if(null == instance) {
                instance = new CDActivityController();
            }
            return instance;
        }
    }

    private final Map<BackgroundAction, CDTaskController> tasks
            = Collections.synchronizedMap(new LinkedHashMap<BackgroundAction, CDTaskController>());

    private CDActivityController() {
        this.loadBundle();
        // Initialize to listen for background tasks
        this.init();
    }

    private void init() {
        BackgroundActionRegistry.instance().addListener(new AbstractCollectionListener<BackgroundAction>() {
            public void collectionItemAdded(final BackgroundAction action) {
                CDMainApplication.invoke(new WindowMainAction(CDActivityController.this) {
                    public void run() {
                        log.debug("collectionItemAdded" + action);
                        tasks.put(action, new CDTaskController(action));
                        reload();
                    }
                });
            }

            public void collectionItemRemoved(final BackgroundAction action) {
                CDMainApplication.invoke(new WindowMainAction(CDActivityController.this) {
                    public void run() {
                        log.debug("collectionItemRemoved" + action);
                        final CDTaskController controller = tasks.remove(action);
                        if(controller != null) {
                            controller.invalidate();
                        }
                        reload();
                    }
                });
            }
        });
        // Add already running background actions
        final BackgroundAction[] actions = BackgroundActionRegistry.instance().toArray(
                new BackgroundAction[BackgroundActionRegistry.instance().size()]);
        for(final BackgroundAction action : actions) {
            tasks.put(action, new CDTaskController(action));
        }
        this.reload();
    }

    private void reload() {
        while(table.subviews().count() > 0) {
            (Rococoa.cast(table.subviews().lastObject(), NSView.class)).removeFromSuperviewWithoutNeedingDisplay();
        }
        table.reloadData();
    }

    public void setWindow(NSWindow window) {
        this.window = window;
        this.window.setReleasedWhenClosed(false);
        this.window.setDelegate(this.id());
        this.window.setTitle(Locale.localizedString("Activity", ""));
    }

    /**
     * @param notification
     */
    public void windowWillClose(NSNotification notification) {
        // Do not call super as we are a singleton. super#windowWillClose would invalidate me
    }

    private NSTableView table;
    private CDAbstractTableDelegate<CDTaskController> delegate;

    public void setTable(NSTableView table) {
        this.table = table;
        this.table.setDataSource(this.id());
        this.table.setDelegate((this.delegate = new CDAbstractTableDelegate<CDTaskController>() {
            @Override
            public void enterKeyPressed(NSObject sender) {

            }

            @Override
            public void deleteKeyPressed(NSObject sender) {

            }

            public String tooltip(CDTaskController c) {
                return null;
            }

            public boolean tableViewShouldSelectRow(NSTableView view, int row) {
                return false;
            }

            public void tableColumnClicked(NSTableView view, NSTableColumn tableColumn) {

            }

            @Override
            public void tableRowDoubleClicked(NSObject sender) {
            }

            public void selectionDidChange(NSNotification notification) {

            }
        }).id());
        {
            NSTableColumn c = NSTableColumn.Factory.create("Default");
            c.setMinWidth(80f);
            c.setWidth(300f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setDataCell(CDControllerCell.Factory.create());
            this.table.addTableColumn(c);
        }
        this.table.sizeToFit();
    }

    public void awakeFromNib() {
        ;
    }

    protected String getBundleName() {
        return "Activity";
    }

    /**
     * @param view
     */
    public int numberOfRowsInTableView(NSTableView view) {
        synchronized(tasks) {
            return tasks.size();
        }
    }

    /**
     * @param view
     * @param tableColumn
     * @param row
     */
    @Override
    public NSObject tableView_objectValueForTableColumn_row(NSTableView view, NSTableColumn tableColumn, int row) {
        if(row < this.numberOfRowsInTableView(view)) {
            final Collection<CDTaskController> values = tasks.values();
            return values.toArray(new CDTaskController[values.size()])[row].view();
        }
        log.warn("tableViewObjectValueForLocation:" + row + " == null");
        return null;
    }
}