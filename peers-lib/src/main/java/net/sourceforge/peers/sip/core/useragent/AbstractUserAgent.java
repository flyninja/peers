package net.sourceforge.peers.sip.core.useragent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.media.MediaManager;
import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.sdp.SDPManager;
import net.sourceforge.peers.sip.core.useragent.handlers.ByeHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.CancelHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.InviteHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.OptionsHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.RegisterHandler;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.TransportManager;

/**
 * This class provide User agent API for configure
 *
 * @author Vitaly Alekseev
 * @since 4/30/2020
 */
public abstract class AbstractUserAgent implements IUserAgent {

    private final List<String> peers = Collections.synchronizedList(new ArrayList<>());

    protected abstract Logger getLogger();

    protected abstract InviteHandler getInviteHandler();

    protected abstract OptionsHandler getOptionsHandler();

    protected abstract CancelHandler getCancelHandler();

    protected abstract ByeHandler getByeHandler();

    protected abstract RegisterHandler getRegisterHandler();

    protected abstract InitialRequestManager getInitialRequestManager();

    protected abstract MidDialogRequestManager getMidDialogRequestManager();

    public final List<String> getPeers() {
        return peers;
    }

    public int getRtpPort() {
        return getConfig().getRtpPort();
    }

    public String getDomain() {
        return getConfig().getDomain();
    }

    public String getUserpart() {
        return getConfig().getUserPart();
    }

    public MediaMode getMediaMode() {
        return getConfig().getMediaMode();
    }

    public boolean isMediaDebug() {
        return getConfig().isMediaDebug();
    }

    public SipURI getOutboundProxy() {
        return getConfig().getOutboundProxy();
    }

    public int getSipPort() {
        return getTransportManager().getSipPort();
    }

    protected DialogManager createDialogManager() {
        return new DialogManager(getLogger());
    }

    protected TransactionManager createTransactionManager() {
        return new TransactionManager(getLogger());
    }

    protected TransportManager createTransportManager() {
        final TransportManager result = new TransportManager(getTransactionManager(), getConfig(), getLogger());
        getTransactionManager().setTransportManager(result);
        return result;
    }

    protected InitialRequestManager createInitialRequestManager() {
        return new InitialRequestManager(
                this,
                getInviteHandler(),
                getCancelHandler(),
                getByeHandler(),
                getOptionsHandler(),
                getRegisterHandler(),
                getDialogManager(),
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected MidDialogRequestManager createMidDialogRequestManager() {
        return new MidDialogRequestManager(
                this,
                getInviteHandler(),
                getCancelHandler(),
                getByeHandler(),
                getOptionsHandler(),
                getRegisterHandler(),
                getDialogManager(),
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected ChallengeManager createChallengeManager() {
        final ChallengeManager result = new ChallengeManager(getConfig(),
                getInitialRequestManager(),
                getMidDialogRequestManager(),
                getDialogManager(),
                getLogger());
        getRegisterHandler().setChallengeManager(result);
        getInviteHandler().setChallengeManager(result);
        getByeHandler().setChallengeManager(result);
        return result;
    }

    protected SDPManager createSDPManager() {
        final SDPManager result = new SDPManager(this, getLogger());
        getInviteHandler().setSdpManager(result);
        getOptionsHandler().setSdpManager(result);
        return result;
    }

    protected MediaManager createMediaManager() {
        return new MediaManager(this, this, getLogger());
    }

    protected InviteHandler createInviteHandler() {
        return new InviteHandler(this,
                getDialogManager(),
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected CancelHandler createCancelHandler() {
        return new CancelHandler(this,
                getDialogManager(),
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected ByeHandler createByeHandler() {
        return new ByeHandler(this,
                getDialogManager(),
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected OptionsHandler createOptionsHandler() {
        return new OptionsHandler(this,
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }

    protected RegisterHandler createRegisterHandler() {
        return new RegisterHandler(this,
                getTransactionManager(),
                getTransportManager(),
                getLogger());
    }
}
