package net.sourceforge.peers.sip.core.useragent;

import java.io.Closeable;
import java.util.List;
import net.sourceforge.peers.Config;
import net.sourceforge.peers.media.AbstractSoundManagerFactory;
import net.sourceforge.peers.media.DtmfEventHandler;
import net.sourceforge.peers.media.Echo;
import net.sourceforge.peers.media.MediaManager;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.TransportManager;

/**
 * This interface provide User agent API
 *
 * @author Vitaly Alekseev
 * @since 4/30/2020
 */
public interface IUserAgent extends Closeable, DtmfEventHandler {

    int RTP_DEFAULT_PORT = 8000;

    UAS getUas();

    UAC getUac();


    Config getConfig();

    List<String> getPeers();

    String getPeersHome();


    TransactionManager getTransactionManager();

    TransportManager getTransportManager();

    MediaManager getMediaManager();

    SipListener getSipListener();

    DialogManager getDialogManager();

    AbstractSoundManagerFactory getAbstractSoundManagerFactory();


    SipRequest register() throws SipUriSyntaxException;

    void unregister() throws SipUriSyntaxException;

    SipRequest invite(String requestUri, String callId) throws SipUriSyntaxException;

    void terminate(SipRequest sipRequest);

    void acceptCall(SipRequest sipRequest, Dialog dialog);

    void rejectCall(SipRequest sipRequest);
    boolean isRegistered();


    String generateCSeq(String method);
    SipRequest getSipRequest(SipMessage sipMessage);

    Echo getEcho();
    void setEcho(Echo echo);
}
