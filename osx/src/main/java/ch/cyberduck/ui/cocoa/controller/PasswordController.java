package ch.cyberduck.ui.cocoa.controller;

/*
 * Copyright (c) 2002-2016 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.binding.AlertController;
import ch.cyberduck.binding.Outlet;
import ch.cyberduck.binding.WindowController;
import ch.cyberduck.binding.application.NSAlert;
import ch.cyberduck.binding.application.NSCell;
import ch.cyberduck.binding.application.NSImage;
import ch.cyberduck.binding.application.NSSecureTextField;
import ch.cyberduck.binding.application.NSTextField;
import ch.cyberduck.binding.application.NSView;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.LocaleFactory;
import ch.cyberduck.core.LoginOptions;
import ch.cyberduck.core.local.BrowserLauncherFactory;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.resources.IconCacheFactory;

import org.apache.commons.lang3.StringUtils;
import org.rococoa.cocoa.foundation.NSRect;

public class PasswordController extends AlertController {

    @Outlet
    protected final NSTextField inputField
            = NSSecureTextField.textfieldWithFrame(new NSRect(0, 22));

    private final Credentials credentials;

    public PasswordController(final WindowController parent, final Credentials credentials,
                              final String title, final String reason, final LoginOptions options) {
        super(parent, NSAlert.alert(
                title,
                reason,
                LocaleFactory.localizedString("Create", "File"),
                null,
                LocaleFactory.localizedString("Cancel", "Alert")
        ), NSAlert.NSInformationalAlertStyle);
        this.credentials = credentials;
        alert.setIcon(IconCacheFactory.<NSImage>get().iconNamed(options.icon, 64));
        alert.setShowsSuppressionButton(options.keychain);
        alert.suppressionButton().setTitle(LocaleFactory.localizedString("Save Password", "Keychain"));
        alert.setShowsHelp(true);
    }

    @Override
    public NSView getAccessoryView() {
        return inputField;
    }

    @Override
    protected void focus() {
        super.focus();
        inputField.selectText(null);
    }

    @Override
    public void callback(final int returncode) {
        if(returncode == DEFAULT_OPTION) {
            credentials.setPassword(inputField.stringValue());
            credentials.setSaved(alert.suppressionButton().state() == NSCell.NSOnState);
        }
    }

    @Override
    public boolean validate() {
        return StringUtils.isNotBlank(inputField.stringValue());
    }

    @Override
    protected void help() {
        final StringBuilder site = new StringBuilder(PreferencesFactory.get().getProperty("website.help"));
        site.append("/howto/cryptomator");
        BrowserLauncherFactory.get().open(site.toString());
    }
}
