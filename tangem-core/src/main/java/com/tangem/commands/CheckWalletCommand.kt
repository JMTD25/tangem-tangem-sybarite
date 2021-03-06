package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

/**
 * Deserialized response from the Tangem card after [CheckWalletCommand].
 *
 * @property cardId Unique Tangem card ID number
 * @property salt Random salt generated by the card.
 * @property walletSignature Challenge and salt signed with the wallet private key.
 */
class CheckWalletResponse(
        val cardId: String,
        val salt: ByteArray,
        val walletSignature: ByteArray
) : CommandResponse

/**
 * This command proves that the wallet private key from the card corresponds to the wallet public key.
 * Standard challenge/response scheme is used.
 *
 * @property pin1 Hashed user’s pin 1 code to access the card. Default unhashed value: ‘000000’.
 * @property cardId Unique Tangem card ID number
 * @property challenge Random challenge generated by application
 */
class CheckWalletCommand(
        private val pin1: String,
        private val cardId: String,
        private val challenge: ByteArray
) : CommandSerializer<CheckWalletResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvData = listOf(
                Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()),
                Tlv(TlvTag.CardId, cardId.hexToBytes()),
                Tlv(TlvTag.Challenge, challenge)
        )
        return CommandApdu(Instruction.CheckWallet, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): CheckWalletResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            CheckWalletResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    salt = mapper.map(TlvTag.Salt),
                    walletSignature = mapper.map(TlvTag.Signature)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}