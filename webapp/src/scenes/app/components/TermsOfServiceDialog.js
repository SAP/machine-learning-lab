import React, { Component } from 'react';
import PropTypes from 'prop-types';
import MarkdownIt from 'markdown-it'
import md5 from 'crypto-js/md5';

import CustomDialog from '../../../components/CustomDialog';
import { administrationApi, getDefaultApiCallback } from '../../../services/client/ml-lab-api';

const md = new MarkdownIt();

class TermsOfServiceDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      open: false,
      tosText: "Initial TOS"
    };
    this.requestTOS();
    this.acceptedTosList = JSON.parse(localStorage.getItem("acceptedTOS") || '[]');
  }

  requestTOS() {
    administrationApi.getLabInfo(null,
      getDefaultApiCallback(
        ({ result }) => {
          let tosText = result.data.termsOfService;
          let tosDisabled = tosText == "ToS disabled\n"
          let alreadyAccepted = this.acceptedTosList.includes(String(md5(tosText)))
          if (!tosDisabled && !alreadyAccepted) {
            this.setState({ tosText, open: true })
          }
        },
        ({ httpResponse }) => {
          let errorMessage = '';
          try {
            errorMessage = httpResponse.body.errors.message;
          } catch (err) {
            // do nothing
          }
          console.error("Error retreiving ToS: " + errorMessage)
        }
      )
    );
  }

  handleAccept() {
    this.acceptedTosList.push(String(md5(this.state.tosText)))
    localStorage.setItem("acceptedTOS", JSON.stringify(this.acceptedTosList))
    this.setState({ open: false });
  }

  renderMarkdownText() {
    return { __html: md.render(this.state.tosText) }
  }

  render() {
    const renderedMarkdown = <div dangerouslySetInnerHTML={this.renderMarkdownText()} />
    return (
      <CustomDialog
        open={this.state.open}
        title={"Terms of Service"}
        contentText={renderedMarkdown}
        hideCancelBtn={true}
        primaryActionBtnDisabled={false}
        primaryActionBtnLabel={"Accept"}
        handlePrimaryAction={() => this.handleAccept()}>
      </CustomDialog>
    );
  }
}

export default TermsOfServiceDialog;
