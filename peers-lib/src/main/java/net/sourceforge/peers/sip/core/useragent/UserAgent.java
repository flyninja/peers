/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008, 2009, 2010 Yohann Martineau 
*/

package net.sourceforge.peers.sip.core.useragent;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.FileLogger;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.XmlConfig;
import net.sourceforge.peers.media.*;
import net.sourceforge.peers.rtp.RFC4733;
import net.sourceforge.peers.sdp.SDPManager;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.handlers.ByeHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.CancelHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.InviteHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.OptionsHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.RegisterHandler;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transaction.Transaction;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;

import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class UserAgent extends AbstractUserAgent {

    public final static String CONFIG_FILE = "conf" + File.separator + "peers.xml";

    private final String peersHome;
    private final Logger logger;
    private final Config config;

    //private List<Dialog> dialogs;
    
    //TODO factorize echo and captureRtpSender
    private Echo echo;
    
    private final UAC uac;
    private final UAS uas;

    private final ChallengeManager challengeManager;
    
    private final DialogManager dialogManager;
    private final TransactionManager transactionManager;
    private final TransportManager transportManager;
    private final InitialRequestManager initialRequestManager;
    private final MidDialogRequestManager midDialogRequestManager;

    private final InviteHandler inviteHandler;
    private final OptionsHandler optionsHandler;
    private final CancelHandler cancelHandler;
    private final ByeHandler byeHandler;
    private final RegisterHandler registerHandler;

    private int cseqCounter;
    private final AbstractSoundManagerFactory abstractSoundManagerFactory;
    private final SipListener sipListener;
    
    private final SDPManager sdpManager;
    private final MediaManager mediaManager;

    public UserAgent(SipListener sipListener, String peersHome, Logger logger)
                    throws SocketException {
        this(sipListener, null, peersHome, logger);
    }

    public UserAgent(SipListener sipListener, Config config, Logger logger)
                    throws SocketException {
        this(sipListener, config, null, logger);
    }

    private UserAgent(SipListener sipListener, Config config, String peersHome, Logger logger)
            throws SocketException {
        this(sipListener, null, config, peersHome, logger);
    }

    public UserAgent(SipListener sipListener, AbstractSoundManagerFactory abstractSoundManagerFactory, Config config, String peersHome, Logger logger)
                    throws SocketException {
        this.sipListener = sipListener;
        if (peersHome == null) {
            peersHome = Utils.DEFAULT_PEERS_HOME;
        }
        this.peersHome = peersHome;
        if (logger == null) {
            logger = new FileLogger(this.peersHome);
        }
        this.logger = logger;
        if (config == null) {
            config = new XmlConfig(this.peersHome + File.separator
                    + CONFIG_FILE, this.logger);
        }
        this.config = config;
        if (abstractSoundManagerFactory == null) {
            abstractSoundManagerFactory = new ConfigAbstractSoundManagerFactory(this.config, this.peersHome, this.logger);
        }
        this.abstractSoundManagerFactory = abstractSoundManagerFactory;

        cseqCounter = 1;
        
        StringBuffer buf = new StringBuffer();
        buf.append("starting user agent [");
        buf.append("myAddress: ");
        buf.append(config.getLocalInetAddress().getHostAddress()).append(", ");
        buf.append("sipPort: ");
        buf.append(config.getSipPort()).append(", ");
        buf.append("userpart: ");
        buf.append(config.getUserPart()).append(", ");
        buf.append("domain: ");
        buf.append(config.getDomain()).append("]");
        logger.info(buf.toString());

        //transaction user
        
        dialogManager = createDialogManager();
        
        //transaction
        
        transactionManager = createTransactionManager();
        
        //transport
        
        transportManager = createTransportManager();
        
        //core
        
        inviteHandler = createInviteHandler();
        cancelHandler = createCancelHandler();
        byeHandler = createByeHandler();
        optionsHandler = createOptionsHandler();
        registerHandler = createRegisterHandler();
        
        initialRequestManager = createInitialRequestManager();
        midDialogRequestManager = createMidDialogRequestManager();
        
        uas = new UAS(this,
                initialRequestManager,
                midDialogRequestManager,
                dialogManager,
                transactionManager,
                transportManager);

        uac = new UAC(this,
                initialRequestManager,
                midDialogRequestManager,
                dialogManager,
                transactionManager,
                transportManager,
                logger);

        challengeManager = createChallengeManager();

        //dialogs = new ArrayList<Dialog>();

        sdpManager = createSDPManager();
        mediaManager = createMediaManager();
    }
    
    // client methods

    @Override
    public void close() {
        transportManager.closeTransports();
        transactionManager.closeTimers();
        inviteHandler.closeTimers();
        mediaManager.stopSession();
        config.setPublicInetAddress(null);
    }

    @Override
    public SipRequest register() throws SipUriSyntaxException {
        return uac.register();
    }

    @Override
    public void unregister() throws SipUriSyntaxException {
        uac.unregister();
    }

    @Override
    public SipRequest invite(String requestUri, String callId)
            throws SipUriSyntaxException {
        return uac.invite(requestUri, callId);
    }

    @Override
    public void terminate(SipRequest sipRequest) {
        uac.terminate(sipRequest);
    }

    @Override
    public void acceptCall(SipRequest sipRequest, Dialog dialog) {
        uas.acceptCall(sipRequest, dialog);
    }

    @Override
    public void rejectCall(SipRequest sipRequest) {
        uas.rejectCall(sipRequest);
    }
    
    
    /**
     * Gives the sipMessage if sipMessage is a SipRequest or 
     * the SipRequest corresponding to the SipResponse
     * if sipMessage is a SipResponse
     * @param sipMessage
     * @return null if sipMessage is neither a SipRequest neither a SipResponse
     */
    @Override
    public SipRequest getSipRequest(SipMessage sipMessage) {
        if (sipMessage instanceof SipRequest) {
            return (SipRequest) sipMessage;
        } else if (sipMessage instanceof SipResponse) {
            SipResponse sipResponse = (SipResponse) sipMessage;
            Transaction transaction = (Transaction)transactionManager
                .getClientTransaction(sipResponse);
            if (transaction == null) {
                transaction = (Transaction)transactionManager
                    .getServerTransaction(sipResponse);
            }
            if (transaction == null) {
                return null;
            }
            return transaction.getRequest();
        } else {
            return null;
        }
    }
    
//    public List<Dialog> getDialogs() {
//        return dialogs;
//    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected InviteHandler getInviteHandler() {
        return inviteHandler;
    }

    @Override
    protected OptionsHandler getOptionsHandler() {
        return optionsHandler;
    }

    @Override
    protected CancelHandler getCancelHandler() {
        return cancelHandler;
    }

    @Override
    protected ByeHandler getByeHandler() {
        return byeHandler;
    }

    @Override
    protected RegisterHandler getRegisterHandler() {
        return registerHandler;
    }

    @Override
    protected InitialRequestManager getInitialRequestManager() {
        return initialRequestManager;
    }

    @Override
    protected MidDialogRequestManager getMidDialogRequestManager() {
        return midDialogRequestManager;
    }


//    public Dialog getDialog(String peer) {
//        for (Dialog dialog : dialogs) {
//            String remoteUri = dialog.getRemoteUri();
//            if (remoteUri != null) {
//                if (remoteUri.contains(peer)) {
//                    return dialog;
//                }
//            }
//        }
//        return null;
//    }

    @Override
    public String generateCSeq(String method) {
        StringBuffer buf = new StringBuffer();
        buf.append(cseqCounter++);
        buf.append(' ');
        buf.append(method);
        return buf.toString();
    }

    @Override
    public boolean isRegistered() {
        return uac.getInitialRequestManager().getRegisterHandler()
            .isRegistered();
    }

    @Override
    public UAS getUas() {
        return uas;
    }

    @Override
    public UAC getUac() {
        return uac;
    }

    @Override
    public DialogManager getDialogManager() {
        return dialogManager;
    }

    @Override
    public Echo getEcho() {
        return echo;
    }

    @Override
    public void setEcho(Echo echo) {
        this.echo = echo;
    }

    @Override
    public AbstractSoundManagerFactory getAbstractSoundManagerFactory() {
        return abstractSoundManagerFactory;
    }

    @Override
    public SipListener getSipListener() {
        return sipListener;
    }

    @Override
    public MediaManager getMediaManager() {
        return mediaManager;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public String getPeersHome() {
        return peersHome;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public TransportManager getTransportManager() {
        return transportManager;
    }

    @Override
    public void dtmfDetected(RFC4733.DTMFEvent dtmfEvent, int duration) {
        sipListener.dtmfEvent(dtmfEvent, duration);
    }
}
